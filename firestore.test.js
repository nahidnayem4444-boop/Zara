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

test("Unauthenticated user: cannot read posts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("posts").get());
});

test("Unauthenticated user: cannot create post", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("posts").doc("post1").set({
      postId: "post1",
      authorId: "any",
      authorUsername: "user",
      authorDisplayName: "User",
      content: "Hello",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: can create their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice",
      displayName: "Alice Wonderland",
      bio: "Hello world",
      avatarUrl: null,
      isPrivate: false,
      isOnline: true,
      followersCount: 0,
      followingCount: 0,
      postsCount: 0,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: cannot create profile for another UID", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      username: "bob",
      displayName: "Bob Builder",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: can create and delete their own post", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const postRef = aliceDb.collection("posts").doc("post_alice_1");
  await assertSucceeds(
    postRef.set({
      postId: "post_alice_1",
      authorId: ALICE_UID,
      authorUsername: "alice",
      authorDisplayName: "Alice",
      content: "First post on ZARA!",
      likesCount: 0,
      commentsCount: 0,
      repostsCount: 0,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  // Bob cannot delete Alice's post
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("posts").doc("post_alice_1").delete());

  // Alice can delete her own post
  await assertSucceeds(postRef.delete());
});

test("Conversations: non-participant cannot read conversation", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("conversations").doc("conv1").set({
      conversationId: "conv1",
      participantIds: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const charlieDb = testEnv.authenticatedContext("charlie_789").firestore();
  await assertFails(charlieDb.collection("conversations").doc("conv1").get());

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("conversations").doc("conv1").get());
});

test("Comments: authenticated user can create comment on post", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("comments").doc("cmt_1").set({
      commentId: "cmt_1",
      postId: "post_1",
      authorId: ALICE_UID,
      authorUsername: "alice",
      authorDisplayName: "Alice",
      content: "Nice post!",
      createdAt: new Date(),
    })
  );
});

test("Messages: non-participant cannot read message", async () => {
  const charlieDb = testEnv.authenticatedContext("charlie_789").firestore();
  await assertFails(
    charlieDb.collection("messages").doc("msg_1").set({
      messageId: "msg_1",
      conversationId: "conv1",
      participantIds: [ALICE_UID, BOB_UID],
      senderId: "charlie_789",
      senderName: "Charlie",
      text: "Sneaky message",
      status: "sent",
      createdAt: new Date(),
    })
  );
});
