pipeline {
    agent any
    environment {
        bundle_name = "${sh(returnStdout: true, script: 'echo "bundle_`date +%Y-%m-%d_%H-%m-%S`"').trim()}"
        repo_folder_name = "dss-project-cicd"
    }
    stages {
        //stage('test') {
        //    agent {
        //       docker {
        //            image 'python/3.7.16-alpine3.17'
        //        }
        //    }
        //    steps {
        //        sh 'virtualenv venv && . venv/bin/activate && pip install -r requirements.txt && python 1 + 1'
        //    }
        //}
        stage('PREPARE'){
            steps {
                cleanWs()
                sh 'echo ${bundle_name}'
                //sh "cat requirements.txt"
                //git credentialsId: "git_hub_ssh", url: "git@github.com:PierrePetrella/dss-project-cicd.git"

                sh "git clone ${GIT_REPO}"
                //sh "ls -la"
                //sh "cd dss-project-cicd"
                //sh "ls -la"
                //sh "cat requirements.txt"
                withPythonEnv('/Users/pierrepetrella/.pyenv/shims/python') {
                    sh "pip install -U pip"
                    sh "pip install -r ${repo_folder_name}/requirements.txt"
                    sh "pip freeze"
                }
            }
        }
        stage('PROJECT_VALIDATION') {
            steps {
                withPythonEnv('/Users/pierrepetrella/.pyenv/shims/python') {
                    sh "pytest -s ${repo_folder_name}/1_project_validation/run_test.py -o junit_family=xunit1 --host='${DESIGN_URL}' --api='${DESIGN_API_KEY}' --project='${DSS_PROJECT}' --junitxml=reports/PROJECT_VALIDATION.xml"
                }
            }
        }
        stage('PACKAGE_BUNDLE') {
            steps {
                withPythonEnv('/Users/pierrepetrella/.pyenv/shims/python') {
                    sh "python ${repo_folder_name}/2_package_bundle/run_bundling.py '${DESIGN_URL}' '${DESIGN_API_KEY}' '${DSS_PROJECT}' ${bundle_name}"
                }
                sh "echo DSS project bundle created and downloaded in local workspace"
                sh "ls -la"
                //script {
                //    def server = Artifactory.server 'artifactory'
                //    def uploadSpec = """{
                //        "files": [{
                //          "pattern": "*.zip",
                //          "target": "generic-local/dss_bundle/"
                //        }]
                //    }"""
                //    def buildInfo = server.upload spec: uploadSpec, failNoOp: true
                //}
            }
        }
        stage('PREPROD_TEST') {
            steps {
                withPythonEnv('/Users/pierrepetrella/.pyenv/shims/python') {
                    sh "python ${repo_folder_name}/3_preprod_test/import_bundle.py '${DESIGN_URL}' '${DESIGN_API_KEY}' '${DEPLOYER_URL}' '${DEPLOYER_API_KEY}' '${DSS_PROJECT}' ${bundle_name} '${AUTO_PREPROD_ID}'"
                    sh "pytest -s ${repo_folder_name}/3_preprod_test/run_test.py -o junit_family=xunit1 --host='${AUTO_PREPROD_URL}' --api='${AUTO_PREPROD_API_KEY}' --project='${DSS_PROJECT}' --junitxml=reports/PREPROD_TEST.xml"
                }                
            }
        }
        stage('DEPLOY_TO_PROD') {
            steps {
                withPythonEnv('/Users/pierrepetrella/.pyenv/shims/python') {
                    sh "python ${repo_folder_name}/4_deploy_prod/deploy_bundle.py '${DESIGN_URL}' '${DESIGN_API_KEY}' '${DSS_PROJECT}' '${bundle_name}' '${AUTO_PROD_ID}' ${AUTO_PROD_URL} ${AUTO_PROD_API_KEY}"
                }
            }
        }
    }
    post{
        always {
            fileOperations ([fileDeleteOperation(includes: '*.zip')])
            junit 'reports/**/*.xml'
      }
    }
}
