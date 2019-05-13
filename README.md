
```bash
aws cloudformation create-stack \
    --capabilities CAPABILITY_NAMED_IAM \
    --stack-name dataplatform-example \
    --template-body file://cloudformation/platform.yaml \
    --parameters \
        ParameterKey=DataPlatformID,ParameterValue=dataplatform001
```