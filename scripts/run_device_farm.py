import argparse
import boto3
import time
import os
import sys
import requests

def run_device_farm(app_path, test_path, project_arn, device_pool_arn):
    client = boto3.client('devicefarm', region_name='us-west-2') # Device Farm is typically us-west-2

    print(f"Uploading App: {app_path}")
    app_upload_arn = upload_file(client, project_arn, app_path, 'ANDROID_APP')

    print(f"Uploading Tests: {test_path}")
    test_upload_arn = upload_file(client, project_arn, test_path, 'INSTRUMENTATION_TEST_PACKAGE')

    print("Scheduling Run...")
    run_result = client.schedule_run(
        projectArn=project_arn,
        appArn=app_upload_arn,
        devicePoolArn=device_pool_arn,
        name=f"Locus-Validation-{int(time.time())}",
        test={
            'type': 'INSTRUMENTATION',
            'testPackageArn': test_upload_arn
        }
    )
    run_arn = run_result['run']['arn']
    print(f"Run Scheduled: {run_arn}")

    # Poll status
    while True:
        run = client.get_run(arn=run_arn)['run']
        status = run['status']
        result = run['result']
        print(f"Status: {status} | Result: {result}")

        if status in ['COMPLETED', 'ERRORED']:
            break

        time.sleep(30)

    # Download Artifacts (Simplified)
    print("Downloading Artifacts...")
    jobs = client.list_jobs(arn=run_arn)['jobs']
    os.makedirs("test_results", exist_ok=True)

    for job in jobs:
        # This is a simplification. Real implementation would traverse suites/tests/artifacts.
        # Just printing job result for now.
        print(f"Job {job['name']}: {job['result']}")

    if result != 'PASSED':
        print("Test Run Failed.")
        sys.exit(1)

    print("Test Run Passed.")

def upload_file(client, project_arn, file_path, type):
    name = os.path.basename(file_path)
    response = client.create_upload(
        projectArn=project_arn,
        name=name,
        type=type,
        contentType='application/octet-stream'
    )
    upload_arn = response['upload']['arn']
    url = response['upload']['url']

    with open(file_path, 'rb') as f:
        requests.put(url, data=f)

    # Wait for upload to succeed
    while True:
        upload = client.get_upload(arn=upload_arn)['upload']
        if upload['status'] == 'SUCCEEDED':
            break
        elif upload['status'] == 'FAILED':
            raise Exception(f"Upload failed: {upload['metadata']}")
        time.sleep(5)

    return upload_arn

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Run Device Farm Tests')
    parser.add_argument('--app-path', required=True)
    parser.add_argument('--test-path', required=True)
    parser.add_argument('--project-arn', required=True)
    parser.add_argument('--device-pool-arn', required=True)
    args = parser.parse_args()

    run_device_farm(args.app_path, args.test_path, args.project_arn, args.device_pool_arn)
