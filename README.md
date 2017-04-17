
**Application Architecture** 

There are 3 application components 

* Presentation
* Proxy Server
* Data Store

Presentation <--[IPC]---> Proxy Server <---[IPC]---> Data Store

There is a separation of concern ie ui deals only with ui so on. 

**Implementation Architecture on Android**

Android facilities used 

* Sync Adapter
* Content Provider
* Bound Service
* Service

These components so mentioned are separate applications (apk) and require 
separate installation.  

*Presentation is an Android Activity. 
*Proxy Server is an Android Bound Service 
*Data Store is a Sync Adapter Service coupled with a Bound Service for trigger.
The data store module also implements data content provider, which would be
used proxy server to retrieve data.

Presentation layer has some dependency checks to ensure that dependent application
services are also installed along side. 

Presentation layer loads data via web view. The http request to retrieve data is 
routed to the proxy server.Proxy server service has a Micro http server running at
port 7860 that would handle the incoming request. The request to load data would 
be handle by the proxy server. Proxy server would use the content provider of the
data store service to retrieve the data and serialise it to json payload (as per 
the specification). 

If the data is not available from the data store, proxy server would forward the
request to load data from a remote url (https://api.myjson.com/bins/xefof). The 
json payload so received would be given as response for the current request.
 
Since data is always available in the data store application, for the demo purpose
some means was required to emulate absence of data in the data store. In order to
do that, a toggle button is provide in the presentation app. This toggle button 
would toggle between the two emulation modes. This was done by communicating 
with the proxy server via message brokers (interesting)...

Local data would have the first item name as 

* consectetur

Remote data would have first item name as

* Remote Data Proof

Please refer the code for more information 

**Running from Android Studio**
* Pull the repository.
* Open the project in android studio
* For headless running of proxyserver and datastore services from the android studio, 
  Edit the run configuration of proxyserver and datastore. Set the the Launch option 
  to NOTHING.
* No extra libaries were used, 3 applications are pure Android DNA

**Questions** 

What problems might faced if the requests originating from the Webview are 
SSL encrypted (https), and how you can overcome them ?

**Answer** 

Presentation [ WebView ] <-https-> Proxy Server

Web view when sending https request, it need to verify that the response that it 
received from the proxy server is an actual response from the authentic proxy
server.

For this to work out, a well known certificate authority (CA) has to sign the proxy
server certificate. So that, the verification module on the web view implementation
can verify this certificate.

Either way, even proxy server can do the same ie verify that the request originated
from a known trusted client in the trust key manager. So that it need not serve such a
request. (Just a defensive thought). 

One issue here is web view can't request a https on port 7860 (our implementation).
Since, it doesn't handle ssl hand shake. The request would time out. 

One hacky approach if we want to use the same port for https, would be to deal
it at the byte level (that is looking at first few bytes or something) to identify
that it is https request and handle the ssl hand shakes etc. This is a theory 
will have to experiment.

This would mean that we would have to launch another instance micro server that
handles https on a different port. Web View can then request https there.  
