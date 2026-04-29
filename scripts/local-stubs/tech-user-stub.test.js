const test = require("node:test");
const assert = require("node:assert/strict");

const { defaultProfiles, resolveProfile } = require("./tech-user-stub");

test("should keep happy-path token and support dedicated no-sign token", () => {
  const profiles = defaultProfiles();

  const readyUser = resolveProfile("Bearer mock-tech-token", profiles);
  assert.equal(readyUser.userId, "tech-user-local-001");

  const noSignUser = resolveProfile("Bearer mock-tech-token-no-sign", profiles);
  assert.equal(noSignUser.userId, "tech-user-local-nosign-001");

  const invalidUser = resolveProfile("Bearer unsupported-token", profiles);
  assert.equal(invalidUser, null);
});
