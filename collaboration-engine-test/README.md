# Collaboration Engine Integration Test

## Run IT
1. Install all modules
   ```
    mvn install -DskipTests
   ```

3. Run ITs test for all tech stacks using Local Chrome in Head mode
   ```
    mvn verify -DuseLocalWebDriver
   ```
    or a specific tech stack
   ```
    mvn verify -DuseLocalWebDriver -pl collaboration-engine-test/collaboration-engine-test-cdi
   ```
4. Run ITs using Local Chrome in HeadLess mode
   ```
    mvn verify -DuseLocalWebDriver -Dheadless
   ```
5. Run ITs in Sauce Labs
   - First, you need a valid username and key in SauceLabs
   - Second, you need to install [Sauce Connect Proxy](https://docs.saucelabs.com/secure-connections/sauce-connect/installation/) in your system
   - Then, in one terminal execute `sc` (Sauce Connect)
   ```
    sc -u your_sauce_user -k your_sauce_key
   ```
   - Finally, in other terminal run ITs tests
   ```
    mvn verify -Dsauce.user="your_sauce_user" -Dsauce.sauceAccessKey="your_sauce_key"
   ```
6. Run ITs in a remote Selenium Hub
   - You need a Selenium Hub running, otherwise you can start one by using `docker`
   ```
     docker run --rm -it -p 4444:4444 -p 7900:7900 \
       -e SE_NODE_MAX_SESSIONS=200 -e SE_NODE_OVERRIDE_MAX_SESSIONS=true -e SE_NODE_SESSION_TIMEOUT=40 \
       --shm-size 2g selenium/standalone-chrome:latest
   ```
   - Run the tests
   ```
    mvn verify -Dcom.vaadin.testbench.Parameters.hubHostname=localhost
   ```
