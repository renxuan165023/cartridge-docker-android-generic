// Folders
def workspaceFolderName = "${WORKSPACE_NAME}"
def projectFolderName = "${PROJECT_NAME}"

// Jobs reference
def generateJavaReferenceAppJobs = freeStyleJob(projectFolderName + "/Generate_Reference_App_Jobs")

generateJavaReferenceAppJobs.with{
  parameters{
    stringParam("GIT_REPOSITORY","android-referece-app","Repository name to build the project from.")
    stringParam("SLAVE_LABEL","android-sdk24.4.1","Restrict the jobs to run on a slave.")
    stringParam("NEXUS_ARTEFACT_NAME","android-reference-app","Name with which artefact will be saved in nexus.")
    stringParam("AUTOMATION_TEST_REPOSITORY","android-automation-test","Repository name to run the regression tests from.")
    configure { project ->
      project / 'properties' / 'hudson.model.ParametersDefinitionProperty'/ 'parameterDefinitions' << 'hudson.model.PasswordParameterDefinition' {
        name("FAIRY_API_KEY")
        description("API key to upload the the apk to testfairy.")
        defaultValue("xxxxxxxxxx")
      }
    }
  }
  environmentVariables {
      env('WORKSPACE_NAME',workspaceFolderName)
      env('PROJECT_NAME',projectFolderName)
  }
  wrappers {
    preBuildCleanup()
    injectPasswords()
    maskPasswords()
  }
  steps {
      shell ('''set +x
            |set +e
            |git ls-remote ssh://gerrit.service.adop.consul:29418/${PROJECT_NAME}/${GIT_REPOSITORY} 2> /dev/null
            |ret=$?
            |set -e
            |if [ ${ret} != 0 ]; then
            | echo "Creating gerrit project : ${PROJECT_NAME}/${GIT_REPOSITORY} "
            | ssh -p 29418 gerrit.service.adop.consul gerrit create-project ${PROJECT_NAME}/${GIT_REPOSITORY} --empty-commit
            |else
            | echo "Repository ${PROJECT_NAME}/${GIT_REPOSITORY} exists! Creating jobs..."
            |fi'''.stripMargin())
      dsl {
        text(readFileFromWorkspace('cartridge/jenkins/jobs/dsl/reference_jobs.template'))
      }
  }
}
