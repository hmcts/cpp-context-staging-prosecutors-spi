# CCT-1210 - Defence disclosure feature - APIM and Function Flow

## CPSNotificationFunction
This is required for sending material notification to CPS (3rd party).

Progression context will make a post call to APIM endpoint identified by [link](TranformAndSendCms.txt).  The APIM endpoint will delegate the request to function CPSNotificationFunction

Data flow as follows:
```
Progression 
-> call APIM (notification-cms/v1/transformAndSendCms)
-> redirect to function (CPSNotificationFunction)
-> call APIM (notification-cms/v1/transformAndSendCms)
-> success (200) or failure response cascaded back 
```

CPSNotificationFunction has the following responsibilities:
* Query material context and retrieve file binary as input stream
* Transform payload sent by progression to the format as required by CPS
* Construct multipart/form-data request and send it to APIM endpoint indentified by [link](SendCms.txt) which will then forward the request to CPS (for integrated environments) or the simulator (for environments like STE, DEV, NFT etc)

**Following function properties need to be set:**

`APIM_CPS_NOTIFICATION_ENDPOINT` = https://{environment}/notification-cms/v1/notification/sendCms (or http://{ste,dev,nft env}:8080/simulator/CP/v1/notification/material-notification)

`material-user-id` = {material system user id}

**Following APIM properties need to be set:**

`cps-material-notification-url` - URL of the simulator or actual live URL for 3rd party service

`cps-notification-function-url` - URL of the function (CPSNotificationFunction)


