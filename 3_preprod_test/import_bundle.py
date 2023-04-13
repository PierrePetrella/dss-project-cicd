import dataikuapi
import sys

design_host = sys.argv[1]
design_apiKey = sys.argv[2]
deployer_host = sys.argv[3]
deployer_apiKey = sys.argv[4]
project = sys.argv[5]
bundle_id = sys.argv[6]
infra = sys.argv[7]

client = dataikuapi.DSSClient(design_host,design_apiKey)

deployer_client = dataikuapi.DSSClient(deployer_host,deployer_apiKey)
pdpl = deployer_client.get_projectdeployer()

# Create or update a deployment
print("Searching for existing deployment of '{}' on infra '{}'".format( project , infra))
pdpl_proj = pdpl.get_project(project)
deployments = pdpl_proj.get_status().get_deployments(infra)

if deployments :
    #update
    deployment = deployments[0]
    print("Using existing deployment '{}'".format(deployment.id))
    depl_settings = deployment.get_settings()
    depl_settings.get_raw()['bundleId'] = bundle_id
    depl_settings.save()
else :
    #create
    print("Need to create a new deployment")
    dp_id = pdpl_proj.id + '-on-' + infra
    deployment = pdpl.create_deployment(dp_id, pdpl_proj.id, infra, bundle_id)
    print("New deployment created as {}".format(deployment.id))

print("Deployment ready to update => {}".format(deployment.id))

# Update the automation node

update_exec = deployment.start_update()
print("Update launched -> {}".format(update_exec.get_state()))
update_exec.wait_for_result()
print("  --> Update done with result => " + str(update_exec.get_result()))

print("Deployment done '{}' on infra '{}' with bundle ID '{}' => status '{}'".format(
    deployment.id,
    deployment.get_settings().get_raw()['infraId'],
    deployment.get_settings().get_raw()['bundleId'],
    deployment.get_status().get_health()
))

if deployment.get_status().get_health() == "ERROR" :
    print("Error when deploying the model to preprod, aborting")
    print(deployment.get_status().get_health_messages())
    sys.exit(1)
