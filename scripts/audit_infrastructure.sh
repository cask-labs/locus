#!/bin/bash
set -e

echo "Starting Infrastructure Audit (Tier 4)..."

# 1. Configuration
REGION="${AWS_REGION:-us-east-1}"
TEMPLATE_PATH="docs/technical_discovery/locus-stack.yaml"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
STACK_NAME="locus-audit-${TIMESTAMP}-${RANDOM}"

echo "Stack Name: $STACK_NAME"
echo "Region: $REGION"
echo "Template: $TEMPLATE_PATH"

# 2. Trap-based cleanup (runs even on failure/interrupt)
cleanup() {
    local exit_code=$?
    if [ $exit_code -ne 0 ]; then
        echo "Script failed with exit code $exit_code. Cleaning up stack..."
    else
        echo "Infrastructure Audit Complete - All validations passed!"
    fi

    echo "Cleaning up stack: $STACK_NAME"
    aws cloudformation delete-stack --stack-name "$STACK_NAME" --region "$REGION" 2>/dev/null || true

    # Wait for deletion to complete
    aws cloudformation wait stack-delete-complete --stack-name "$STACK_NAME" --region "$REGION" 2>/dev/null || true

    echo "Stack cleanup complete."
}
trap cleanup EXIT ERR INT TERM

# 3. Validate AWS credentials
if ! aws sts get-caller-identity --region "$REGION" > /dev/null 2>&1; then
    echo "Error: Unable to validate AWS credentials. Please ensure AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, or AWS_PROFILE is set."
    exit 1
fi

# 4. Validate template exists
if [ ! -f "$TEMPLATE_PATH" ]; then
    echo "Error: Template file not found at $TEMPLATE_PATH"
    exit 1
fi

# 5. Deploy CloudFormation stack
echo "Deploying CloudFormation stack..."
aws cloudformation deploy \
    --template-file "$TEMPLATE_PATH" \
    --stack-name "$STACK_NAME" \
    --parameter-overrides "StackName=audit${RANDOM}" \
    --capabilities CAPABILITY_NAMED_IAM \
    --region "$REGION"

# 6. Verify CREATE_COMPLETE status
echo "Verifying stack deployment..."
STACK_STATUS=$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" --region "$REGION" --query 'Stacks[0].StackStatus' --output text)

if [ "$STACK_STATUS" != "CREATE_COMPLETE" ]; then
    echo "Error: Stack deployment failed with status: $STACK_STATUS"
    echo "Stack events:"
    aws cloudformation describe-stack-events --stack-name "$STACK_NAME" --region "$REGION" \
        --query 'StackEvents[?ResourceStatus==`CREATE_FAILED` || ResourceStatus==`UPDATE_FAILED`].[LogicalResourceId,ResourceStatusReason]' \
        --output table || true
    exit 1
fi

# 7. Validate all outputs present
echo "Validating stack outputs..."
OUTPUTS=$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" --region "$REGION" --query 'Stacks[0].Outputs')

if [ "$OUTPUTS" == "null" ] || [ -z "$OUTPUTS" ]; then
    echo "Error: No stack outputs found"
    exit 1
fi

# Extract and display outputs
BUCKET_NAME=$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" --region "$REGION" --query "Stacks[0].Outputs[?OutputKey=='BucketName'].OutputValue" --output text)
BUCKET_ARN=$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" --region "$REGION" --query "Stacks[0].Outputs[?OutputKey=='BucketArn'].OutputValue" --output text)
ACCESS_KEY_ID=$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" --region "$REGION" --query "Stacks[0].Outputs[?OutputKey=='AccessKeyId'].OutputValue" --output text)
SECRET_ACCESS_KEY=$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" --region "$REGION" --query "Stacks[0].Outputs[?OutputKey=='SecretAccessKey'].OutputValue" --output text)

# Verify all outputs are present
if [ -z "$BUCKET_NAME" ] || [ -z "$BUCKET_ARN" ] || [ -z "$ACCESS_KEY_ID" ] || [ -z "$SECRET_ACCESS_KEY" ]; then
    echo "Error: Missing required stack outputs"
    echo "BucketName: $BUCKET_NAME"
    echo "BucketArn: $BUCKET_ARN"
    echo "AccessKeyId: $ACCESS_KEY_ID"
    echo "SecretAccessKey: [present]"
    exit 1
fi

# Display outputs (redact secret)
echo "Stack outputs:"
echo "BucketName: $BUCKET_NAME"
echo "BucketArn: $BUCKET_ARN"
echo "AccessKeyId: $ACCESS_KEY_ID"
echo "SecretAccessKey: [redacted]"

echo "Stack $STACK_NAME will be deleted during cleanup..."
