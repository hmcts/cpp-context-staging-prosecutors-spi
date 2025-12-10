# Azure functions

## CJSEMetaDataFunction
Takes an SPI In XML message and returns an [CJSEMetaData](src/main/java/uk/gov/moj/cpp/casefilter/azure/pojo/CJSEMetaData.java)
When the message is invalid `mdiFailure` variable is set to true. 
With a valid message the value is not set.

## ApplyFilterRules
Used with a GET request. 
If the case is already in CC, we re-send it.
Uses a CVS file that contains the case filter-in rules
Updates the table EjectedOrCaseFilteredCase when we do not send the message to CC.

Returns a BadRequest when the given parameters are invalid.
Returns Ok (body=true) when we want to relay the message to Libra.
Returns Ok (body=false) when we want to send the case to CC.

## SetCaseEjected
Issued when a Court Clerk ejects a case in CC. 
The case is removed from CC and the table EjectedOrCaseFilteredCase is updated with ejected=true.

## RelayCaseToLibraFunction
Alters the destination tag in the XML message.

## Installation instructions to run azure functions locally

1. Install [core tools v2](https://docs.microsoft.com/en-us/azure/azure-functions/functions-run-local?tabs=macos#v2)
2. Install [.net core sdk 2.x not 3.0](https://dotnet.microsoft.com/download/dotnet-core)
3. Install [Azure cli](https://docs.microsoft.com/en-gb/cli/azure/?view=azure-cli-latest)

## Deployment instructions

### Local deploy
* Package: mvn clean package
* Run locally: mvn azure-functions:run
* Deploy to cloud: ```az login && mvn azure-functions:deploy```
  * This will also build a zip file of the azure functions under ```target/azure-functions```

### Jenkins deploy
* visit https://{environment}/view/casefilter/job/STE_casefilter_ccm-jobs_function_deploy.groovy_stagingprosecutors___casefilter/build?delay=0sec) for **STE casefilter - Function Deploy**
* Fill in Build with parameters as below with the rest of the parameters as default
  * ARTIFACT_VERSION: 6.4.15-CASEFILTERSV2-SNAPSHOT
  * AZURE_PAAS_RG: RG-STE-CASEFILTER
  * AZURE_PAAS_APP: fa-ste-casefilter
 * Build will deploy the function.

## Test instructions

### Azure [Portal](https://portal.azure.com/)
#### _Storage account_ **casefilterfapp** 

Here is where the case filter rules file are uploaded/downloaded. 
Use the Storage Explorer preview to upload/download the CSV file. 
The EjectedOrCaseFilteredCase table can also be viewed there. 
A record will exist if a SPI case has been filtered out or has been ejected from CC

#### _Api Management service_ **spnl-apim-int** 
The policy file [APIMPolicy.txt](src/main/resources/APIMPolicy.txt) exists in the following location:
```
APIs -> fa-ste-casefilter -> POST /cjseSubmitCaseAPI -> In bound processing -> base
```
The **Test** tab at the top will allow you to test the policy file. 
Add the [SPI_In_Message](src/test/resources/SPI_In_Message) XML, 
Check the _Bypass CORS proxy_ checkbox and hit send.

The **Response Message** tab will return the status code.

The **Response Trace** tab will explain what functions and http requests the call had made.

### Function Apps **fa-ste-casefilter**

Here you can test the functions through azure portal. Alternatively, you can get the link of each function and issue the requests via curl or Postman. 