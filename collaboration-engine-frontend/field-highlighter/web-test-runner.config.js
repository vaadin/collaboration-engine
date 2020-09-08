module.exports = {
  nodeResolve: true,
  coverage: true,
  coverageConfig: {
    include: ['**/src/**/*'],
    threshold: {
      statements: 99,
      branches: 92,
      functions: 100,
      lines: 99
    }
  },
};
