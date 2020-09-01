# Collaboration Engine Integration Test

## Run IT
1. Install all modules
    ```
    mvn install -DskipTests
   ```

3. Run IT test for all tech stacks
    ```
    mvn verify
    ```
    or a specific tech stack
    ```
    mvn verify -pl collaboration-engine-test/collaboration-engine-test-cdi
    ```
