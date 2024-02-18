This code demonstrates using Spring WebFlux to create a zip file for download through a REST endpoint. 

This example does so using small chunks of memory to create the zip and send it in chunks to the client so that downloads use the same amount of server memory regardless of download size.

