<h1>WPS-Ilwis Bridge</h1>
Wiki page: https://wiki.52north.org/bin/view/Projects/GSoC2015WpsIlwisBridge <br>
Introductory blogpost: http://blog.52north.org/2015/05/27/wps-ilwis-bridge/ <br>
Midterm blogpost: http://blog.52north.org/2015/07/03/wps-ilwis-bridge-midterm-post/ <br>

<h2>Ilwis Process Repository</h2>
<b>Ilwis backend for 52°North WPS.</b>
<h3>Requirements</h3>
<h4>Windows</h4>
<li>2 bit Java 8</li>
<h4>Linux</h4>
<li>64 bit Java 8 - tested with <a href="http://www.webupd8.org/2014/03/how-to-install-oracle-java-8-in-debian.html">this.</a> </li>
<li>Linux: libgdal (1.18.0)</li>

<h3>Set up Ilwis-Objects with Java</h3>
<li>Download for <a href="https://drive.google.com/open?id=0B0bWmJJYoWIpYS1rdWVkbUJnS3c">Windows</a>.</li>
<li>Extract it to eg. <i>C:\ilwisobjects</i>.</li>
<li>Add the ilwisobjects folder to the PATH variable. <a href="http://www.computerhope.com/issues/ch000549.htm">HOWTO</a> <br>
On Linux, use <b>export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/ilwisobjects-location/libraries/genericrelease</b> </li>
<h3>Building the library</h3>
<li>Use <code>mvn clean install</code> in the <i>52n-wps-ilwis</i> folder to export to jar.<br></li>
<h3>Add at WPS build time</h3>
Extend the 52n-wps-webapp <b>pom.xml</b> dependencies:<br>
```xml
<!-- Ilwis dependency -->
		<dependency> 
			<groupId>org.n52.wps</groupId>
			<artifactId>52n-wps-ilwis</artifactId>
		</dependency>
		<dependency>
			<groupId>org.n52.ilwis.java</groupId>
			<artifactId>52n-ilwis-java</artifactId>
		</dependency>
```
Edit and copy the <b>ilwislocation.config</b> to <i>WPS\52n-wps-webapp\src\main\webapp\config</i>.
<h3>Add at WPS runtime</h3>
<li>Copy <b>52n-ilwis-java-0.0.1-SNAPSHOT.jar</b> and <b>52n-wps-ilwis-3.3.2-SNAPSHOT.jar</b> to <i>52n-wps-webapp-3.3.2-SNAPSHOT\WEB-INF\lib</i></li>
<h3>Activate in WPS</h3>
<a href="https://drive.google.com/open?id=0B0bWmJJYoWIpQXA0WkxkNlNxMXM">Image</a><br>
IlwisRepository<br>
org.n52.wps.server.ilwis.IlwisProcessRepository<br>
IlwisLocation	C:/ilwis/ <br>
Algorithm	org.n52.wps.server.ilwis.mirrorrotateraster<br>

<h1>Contact</h1>
Ilwis mailing list: ilwis@52north.org

<h1>License</h1>
This project is published under The Apache Software License, Version 2.0.
