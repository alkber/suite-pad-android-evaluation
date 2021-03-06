
**Application Architecture** 

There are 3 components 

* Presentation 
* Proxy Server
* Data Store

Presentation <--[IPC]---> Proxy Server <---[IPC]---> Data Store

There is a separation of concern. Each component only does what it was delegated to do.

**Implementation Architecture on Android**

Android facilities used 

* Sync Adapter
* Content Provider
* Bound Service
* Service

These components so mentioned are separate applications (apk) and require 
separate installation.  

* Presentation is an Android Activity. 
* Proxy Server is an Android Bound Service 
* Data Store is a Sync Adapter Service coupled with a Bound Service for trigger.
  The data store module also implements data content provider, which would be 
  used proxy server to retrieve data.

Presentation layer has some dependency checks to ensure that dependent application
services are also installed along side. It loads the data via web view. The http 
request to retrieve data is routed through proxy server. 

Proxy server service has a Micro http server running at port 7860 that would handle 
the incoming request. The request to load data would be handle by the proxy server. 
It uses the content provider of the data store service to retrieve the data and 
serialise it to a json payload (as per the specification). 

If the data is not available from the content provided, proxy server would forward 
the request to load data from a remote url (https://api.myjson.com/bins/xefof). The 
json payload so received by the proxy would be given as response for the current
request.
 
Since data is always available in the data store application (for the demo purpose), 
some means were required to emulate absence of data in the data store. To do that
a toggle button is provide in the presentation app. This toggle button would toggle 
between the two emulation modes. This is done by communicating with the proxy server 
via message brokers (gets interesting)...

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

**Source Code Organisation** 

There are 3 modules 

* /common       - library that is being shared accross the 3 project. It has some constants and utility functions.
* /datastore    - data store service application 
* /proxyserver  - proxy server service application
* /presentation - android front end application

**Questions** 

What problems might faced if the requests originating from the Webview are 
SSL encrypted (https), and how you can overcome them ?

**Answer** 

Presentation [ WebView ] <-https-> Proxy Server

Issue here is web view can't request a https on port 7860 (our implementation).
Since, it doesn't handle ssl handshake. The request would time out.

This would mean that we would have to create another SSLSocket listener on a
different thread of our Micro Web Server that handles https on a different port.
Web View can then request https there.  

Web view when sending https request, it need to verify that the response that it 
received from the proxy server is an actual response from the authentic proxy
server.For this to work out, a well known certificate authority (CA) has to sign 
the proxy server certificate. So that, the verification module on the native web view 
implementation can verify this certificate.

Either way, even proxy server can do the same ie verify that the request originated
from a known trusted client in the trust key manager. So that it need not serve such a
request. (Just a defensive thought). 
