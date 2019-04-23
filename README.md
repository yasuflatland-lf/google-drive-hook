# Liferay 7.1/7.0 Google Drive Integration with Documents and Media
This hook is a sample of Google Drive integration with Document and Media

## Requirement
* Liferay 7.0 SP9+
* Liferay 7.1 SP9+

## Repository
For `7.0`, please see `7.0` branch. `development` and `master` are currently for 7.1
## Usage
1. Generate Google Client ID and Google Secret
2. Generate Authorization Code
3. Generate Access Token and Refresh Token
4. Deploy the module and configure it

### How to generate Google Client ID and Google Secret
* Access to the [Google Cloud Platform](https://console.cloud.google.com/home/dashboard). If you've not created a project. Create a project.
* From the left menu, go to API and Service and select Authentication. Click create authentication information button and select OAuth client ID.
* Create Application name (For example, GD service) and add all to the scope, and Save. If you want to keep it minimum, select only "https://www.googleapis.com/auth/drive"
* Select "Other" in OAuth Client ID creation page and set the name "GD Service," and save. It'll show you both the Client ID and Client Secret. Record the generated Client ID and Client Secret. Also please refer [this](https://developers.google.com/fit/android/get-api-key#request_an_oauth_20_client_id_in_the_console_name) document too.
* Go to Library tab and add Google Drive API, then enable Drive API. Moreover, Click "Try this API" and  click "
Authorize requests using OAuth 2.0:", then select all API links and click "Authorize."
* Navigate to Authentication tab and create an Authentication, select Create OAuth Client ID, then select other, then add Name. In this case, we name it GDoc Auth

### How to generate Authorization Code
The detailss of scope, please refer [here](https://developers.google.com/drive/api/v2/about-auth)
1. Replase ```<Google Client ID>``` of the following link and open up ```https://accounts.google.com/o/oauth2/v2/auth?response_type=code&client_id=<Google Client ID>&redirect_uri=urn:ietf:wg:oauth:2.0:oob&scope=https://www.googleapis.com/auth/drive&access_type=offline```
2. Record the Authorization code displayed.

### How to generate Access Token and Refresh Token
Open up a console and replace ```Authorization code```, ```Client ID``` and ```Client Secret``` in the code and run

```
curl --data "code=<Authorization Code>" --data "client_id=<Client ID>" --data "client_secret=<Client Secret>" --data "redirect_uri=urn:ietf:wg:oauth:2.0:oob" --data "grant_type=authorization_code" --data "access_type=offline" https://www.googleapis.com/oauth2/v4/token
```

JSON data will be displayed. Recorde the refresh token.

### How to Deploy the module
Add following properties into portal-ext.properties. For more details, consult [here](https://dev.liferay.com/ja/develop/tutorials/-/knowledge_base/7-0/using-the-wab-generator)
```
module.framework.web.generator.generated.wabs.store=true
module.framework.web.generator.generated.wabs.store.dir=${module.framework.base.dir}/wabs
```

* Startup Liferay server
* Place this hook at ```${liferay_workspace}/wars``` and go to the root directory of the hook, then run ```blade deploy```
* Access to Liferay as Admin and navigate to Control Panel -> Contents -> Documents and Media
* Click plus button and select Repository, open up Repository Configuration accordion and choose Google Drive
* Fill in ```Google Client ID```, ```Google Secret```, ```Access Token``` and ```Refresh Token``` and Save it with a name of the configuration.
* Enjoy! 

## Reference links
* [Liferay Wab Generator](https://dev.liferay.com/ja/develop/tutorials/-/knowledge_base/7-0/using-the-wab-generator)
* [Drive API v2](https://developers.google.com/api-client-library/java/apis/drive/v2)
* [Drive API v2 Explore on browser](https://developers.google.com/apis-explorer/#p/drive/v2/)
* [Google API Java Client Sample](https://github.com/google/google-api-java-client-samples/tree/master/drive-cmdline-sample)