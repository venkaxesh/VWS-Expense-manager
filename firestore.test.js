const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user: cannot read users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
});

test("Unauthenticated user: cannot create expense", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("expenses").doc("exp_1").set({
    id: "exp_1",
    roomId: "room_1",
    title: "WiFi",
    amountPaise: 80000,
    paidByUid: ALICE_UID,
    participantUids: [ALICE_UID, BOB_UID]
  }));
});

test("Alice can create her own user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set({
    uid: ALICE_UID,
    fullName: "Alice Kumar",
    username: "alice",
    email: "alice@example.com"
  }));
});

test("Bob cannot write to Alice user profile", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).set({
    uid: BOB_UID,
    fullName: "Bob Sneaky",
    username: "bob",
    email: "bob@example.com"
  }));
});

test("Alice can create room and add expense", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("rooms").doc("room_1").set({
    id: "room_1",
    name: "Green House",
    adminUid: ALICE_UID,
    memberUids: [ALICE_UID, BOB_UID]
  }));

  await assertSucceeds(aliceDb.collection("expenses").doc("exp_1").set({
    id: "exp_1",
    roomId: "room_1",
    title: "Groceries",
    amountPaise: 125000,
    paidByUid: ALICE_UID,
    participantUids: [ALICE_UID, BOB_UID]
  }));
});
