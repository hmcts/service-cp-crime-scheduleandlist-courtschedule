# Service: Common Platform (CP) Crime Scheduling and Listing Court Schedule

## Documentation

Further documentation see the [HMCTS Marketplace Springboot template readme](https://github.com/hmcts/service-hmcts-marketplace-springboot-template/blob/main/README.md).

### Run pact provider test and publish verification report to pact broker locally

update .env file with below details
PACT_BROKER_URL= <<PactFlow broker url>>
PACT_BROKER_HOST=<<PactFlow broker url excluding https://>>
PACT_BROKER_TOKEN= <<Your PactFlow broker token>>
PACT_ENV= << This is the environment in PactFlow broker to which we tag the contracts>
PACT_VERIFIER_PUBLISH_RESULTS=true
run ./publish-pacts.sh

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
