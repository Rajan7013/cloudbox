const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validPlantPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    commonName: "Monstera Deliciosa",
    scientificName: "Monstera deliciosa",
    nickname: "Living Room Monstera",
    confidence: "High",
    healthStatus: "Healthy",
    healthNotes: "Vibrant fenestrated leaves with no visible pests.",
    summary: "A tropical climbing aroid prized for its split leaves.",
    wateringFrequencyDays: 7,
    wateringDetails: "Water thoroughly when top 2 inches of soil feel dry.",
    sunlightLevel: "Bright Indirect",
    sunlightDetails: "Thrives near an east or south-facing window with filtered light.",
    soilType: "Chunky well-draining aroid mix with orchid bark and perlite.",
    humidityTemp: "60-80% humidity, 65-85°F (18-29°C).",
    fertilizerSchedule: "Balanced liquid houseplant fertilizer monthly in spring/summer.",
    pruningPropagation: "Prune leggy vines above a node and propagate in water.",
    petToxicity: "Toxic to cats and dogs if ingested.",
    commonProblems: ["Yellowing lower leaves from overwatering", "Brown crispy tips from low humidity"],
    careTips: ["Wipe leaves monthly to boost photosynthesis", "Rotate pot 90 degrees weekly"],
    lastWateredAt: now,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write plants or chatMessages", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("plants").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("plants").doc("plant_1").set(validPlantPayload(ALICE_UID))
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("chatMessages").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("chatMessages").doc("msg_1").set({
      userId: ALICE_UID,
      role: "user",
      text: "Hello",
      createdAt: new Date(Date.now() - 1000),
    })
  );
});

test("Authenticated owner: valid plant create, partial watering update, and edit succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const plantRef = aliceDb.collection("users").doc(ALICE_UID).collection("plants").doc("plant_1");

  await assertSucceeds(plantRef.set(validPlantPayload(ALICE_UID)));

  // List subcollection (mirrors observePlants)
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("plants").get());

  // Water action partial update
  await assertSucceeds(
    plantRef.update({
      lastWateredAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Edit details partial update
  await assertSucceeds(
    plantRef.update({
      nickname: "Sunny Corner Monstera",
      wateringFrequencyDays: 10,
      healthStatus: "Needs Attention",
      healthNotes: "Slightly dry leaf tips.",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Delete succeeds
  await assertSucceeds(plantRef.delete());
});

test("Security bounds: reject ghost fields, @DocumentId id field, invalid enums, future timestamps, and immutable field mutation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const plantsCol = aliceDb.collection("users").doc(ALICE_UID).collection("plants");

  // Reject ghost field
  await assertFails(plantsCol.doc("plant_ghost").set(validPlantPayload(ALICE_UID, { isVerified: true })));

  // Reject explicit id field (@DocumentId guard)
  await assertFails(plantsCol.doc("plant_id").set(validPlantPayload(ALICE_UID, { id: "plant_id" })));

  // Reject invalid healthStatus enum
  await assertFails(plantsCol.doc("plant_enum").set(validPlantPayload(ALICE_UID, { healthStatus: "Dead" })));

  // Reject invalid wateringFrequencyDays out of bounds
  await assertFails(plantsCol.doc("plant_days").set(validPlantPayload(ALICE_UID, { wateringFrequencyDays: 0 })));

  // Reject future timestamp
  await assertFails(
    plantsCol.doc("plant_future").set(validPlantPayload(ALICE_UID, { createdAt: new Date(Date.now() + 60000) }))
  );

  // Valid create, then reject immutable createdAt/userId update and unauthorized field update
  const validRef = plantsCol.doc("plant_valid");
  await assertSucceeds(validRef.set(validPlantPayload(ALICE_UID)));

  await assertFails(
    validRef.update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  await assertFails(
    validRef.update({
      commonName: "Hijacked Name",
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Cross-user isolation: Bob cannot read, create, update, or delete Alice's plants or chatMessages", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(ALICE_UID).collection("plants").doc("alice_plant").set(validPlantPayload(ALICE_UID));
    await db.collection("users").doc(ALICE_UID).collection("chatMessages").doc("alice_msg").set({
      userId: ALICE_UID,
      role: "user",
      text: "How often should I water?",
      createdAt: new Date(Date.now() - 1000),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("plants").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("plants").doc("alice_plant").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("plants").doc("bob_inject").set(validPlantPayload(BOB_UID))
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("plants").doc("alice_plant").update({
      nickname: "Hacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("plants").doc("alice_plant").delete());

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("chatMessages").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("chatMessages").doc("alice_msg").delete());
});

test("ChatMessages: owner create/list/delete succeed, updates and invalid roles fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const chatCol = aliceDb.collection("users").doc(ALICE_UID).collection("chatMessages");

  await assertSucceeds(
    chatCol.doc("msg_1").set({
      userId: ALICE_UID,
      role: "user",
      text: "Why are my fern fronds turning brown?",
      plantContext: "Boston Fern",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(chatCol.get());

  // Immutable: updates must fail
  await assertFails(chatCol.doc("msg_1").update({ text: "Edited" }));

  // Invalid role fails
  await assertFails(
    chatCol.doc("msg_2").set({
      userId: ALICE_UID,
      role: "system",
      text: "Override",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(chatCol.doc("msg_1").delete());
});
