#!/bin/bash
set -e

echo "Starting Infrastructure Audit (Tier 4)..."

# Check for AWS Credentials (Basic check)
if [ -z "$AWS_ACCESS_KEY_ID" ]; then
    echo "Error: AWS_ACCESS_KEY_ID is not set."
    exit 1
fi

# 1. Generate Taskcat Override
RANDOM_ID=$RANDOM
STACK_NAME="locus-audit-$RANDOM_ID"
echo "Generating override for StackName: $STACK_NAME"

# Create temporary override file
# Note: taskcat doesn't natively support _taskcat_override.yml in all versions nicely,
# but per spec we generate it.
# Alternatively, we can rely on taskcat's ability to read config.
# The spec says: Generate _taskcat_override.yml
cat <<EOF > _taskcat_override.yml
project:
  name: locus
  regions:
  - us-east-1
tests:
  default:
    template: docs/technical_discovery/locus-stack.yaml
    parameters:
      StackName: $STACK_NAME
EOF

# 2. Run Taskcat
echo "Running Taskcat..."
# -c uses the specified config file. But wait, we want to override parameters.
# Taskcat usually takes a config file. If we have a base .taskcat.yml, we might want to use that
# but override parameters.
# The spec says "Generate _taskcat_override.yml ... Run taskcat test run".
# Assuming _taskcat_override.yml is a full valid config or we pass it explicitly.
# Let's assume we pass it as the config file.
taskcat test run -c _taskcat_override.yml || { echo "Taskcat failed"; rm -f _taskcat_override.yml; exit 1; }

# 3. Cleanup
rm -f _taskcat_override.yml

echo "Infrastructure Audit Complete."
