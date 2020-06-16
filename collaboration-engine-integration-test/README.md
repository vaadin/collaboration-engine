# Integration test for Collaboration Engine

This project contains integration tests for **collaboration-engine** module.  

### Running Integration Tests

Integration tests are implemented using [Vaadin TestBench](https://vaadin.com/testbench).
The tests take a few minutes to run and are therefore included in a separate Maven profile.
To run the tests using Google Chrome, execute
`mvn verify`

We recommend running tests with a production build to minimize the chance of development time toolchains affecting test stability by executing

`mvn verify -Pproduction`

and make sure you have a valid TestBench license installed.

Profile `it` adds the following parameters to run integration tests:
```sh
-Dwebdriver.chrome.driver=path_to_driver
-Dcom.vaadin.testbench.Parameters.runLocally=chrome
```

For a full Vaadin application example, there are more choices available also from [vaadin.com/start](https://vaadin.com/start) page.

