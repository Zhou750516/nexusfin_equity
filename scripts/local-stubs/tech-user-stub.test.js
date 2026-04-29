const test = require("node:test");
const assert = require("node:assert/strict");

const { defaultProfiles, resolveProfile } = require("./tech-user-stub");

test("should keep happy-path token and support dedicated no-sign and clean tokens", () => {
  const profiles = defaultProfiles();

  const readyUser = resolveProfile("Bearer mock-tech-token", profiles);
  assert.equal(readyUser.userId, "tech-user-local-001");

  const noSignUser = resolveProfile("Bearer mock-tech-token-no-sign", profiles);
  assert.equal(noSignUser.userId, "tech-user-local-nosign-001");

  const cleanUser = resolveProfile("Bearer mock-tech-token-clean", profiles);
  assert.equal(cleanUser.userId, "tech-user-local-clean-001");
  assert.notEqual(cleanUser.userId, readyUser.userId);
  assert.notEqual(cleanUser.userId, noSignUser.userId);

  const secondCleanUser = resolveProfile("Bearer mock-tech-token-clean-2", profiles);
  assert.equal(secondCleanUser.userId, "tech-user-local-clean-002");
  assert.notEqual(secondCleanUser.userId, readyUser.userId);
  assert.notEqual(secondCleanUser.userId, noSignUser.userId);
  assert.notEqual(secondCleanUser.userId, cleanUser.userId);

  const invalidUser = resolveProfile("Bearer unsupported-token", profiles);
  assert.equal(invalidUser, null);
});
