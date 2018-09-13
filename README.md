# Liferay 7.0 Google Drive Integration with Documents and Media
This is a sample of Google Drive integration with Document and Media

## Requirement
* Liferay 7.0 SP8+

## Usage
1. Generate Google Client ID and Google Secret
2. Generate Authorization Code
3. Generate Access Token and Refresh Token
4. Deploy the module and configure it

### How to generate Google Client ID and Google Secret
* Access to the [Google Cloud Platform](https://console.cloud.google.com/home/dashboard). If you've not created a project. Create a project. Then generate [Google Client ID and Google Client Secret](https://developers.google.com/fit/android/get-api-key#request_an_oauth_20_client_id_in_the_console_name)
* Go to Libraly tab and add Google Drive API, then enable Drive API.
* Navigate to Authentication tab and create an Authentication, select Create OAuth Client ID, then select other, then add Name. In this case, we name it GDoc Auth
* Record the generated Client ID and Client Secret.

### How to generate Authorization Code
The detailss of scope, please refer [here](https://developers.google.com/drive/api/v2/about-auth)
1. Replase <Google Client ID> of the following link and open up ```https://accounts.google.com/o/oauth2/v2/auth?response_type=code&client_id=<Google Client ID>&redirect_uri=urn:ietf:wg:oauth:2.0:oob&scope=https://www.googleapis.com/auth/drive&access_type=offline```
2. Record the Authorization code displayed.

### How to generate Access Token and Refresh Token
1. Open up a console and replace Authorization code, Client ID and Client Secret in the code and run ```curl --data "code=<Authorization Code>" --data "client_id=<Client ID>" --data "client_secret=<Client Secret>" --data "redirect_uri=urn:ietf:wg:oauth:2.0:oob" --data "grant_type=authorization_code" --data "access_type=offline" https://www.googleapis.com/oauth2/v4/token```
2. JSON data will be displayed. Recorde the refresh token.

### How to Deploy the module
* Start up Liferay server
* Place this hook at ${liferay_workspace}/modules and go to the root directory of hook, then run ```blade deploy```
* Access to Liferay as Admin and navigate to Control Panel -> Contents -> Documents and Media
* Click plus button and select Repository, open up Repository Configuration accordion and choose Googld Drive
* Fill in Google Client ID, Google Secret, Access Token and Refresh Token and Save it with a name of the configration.
* Enjoy! 

## Reference links
* [Drive API v2](https://developers.google.com/api-client-library/java/apis/drive/v2)
* [Drive API v2 Explore on browser](https://developers.google.com/apis-explorer/#p/drive/v2/)
* [Google API Java Client Sample](https://github.com/google/google-api-java-client-samples/tree/master/drive-cmdline-sample)