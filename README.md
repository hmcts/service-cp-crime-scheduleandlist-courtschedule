# Service: Common Platform (CP) Crime Scheduling and Listing Court Schedule

## Documentation

Further documentation see the [HMCTS Marketplace Springboot template readme](https://github.com/hmcts/service-hmcts-marketplace-springboot-template/blob/main/README.md).

## Pact Provider Test

Run pact provider test and publish verification report to pact broker locally

Update .env file with below details (replacing placeholders with actual values):
```bash
export PACT_BROKER_URL="https://hmcts-dts.pactflow.io"
export PACT_BROKER_TOKEN="YOUR_PACTFLOW_BROKER_TOKEN"
export PACT_ENV="local" # or value based on the environment you are testing against
export PACT_VERIFIER_PUBLISH_RESULTS=true
```
Run Pact tests:
```bash
gradle pactVerificationTest
```

### Contribute to This Repository

Contributions are welcome! Please see the [CONTRIBUTING.md](.github/CONTRIBUTING.md) file for guidelines.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
