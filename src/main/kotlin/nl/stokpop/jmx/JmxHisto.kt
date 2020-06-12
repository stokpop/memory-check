/**
 * Copyright (C) 2020 Peter Paul Bakker, Stokpop Software Solutions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.stokpop.jmx

import javax.management.MBeanServerConnection
import javax.management.ObjectName
import javax.management.remote.JMXConnector
import javax.management.remote.JMXConnectorFactory
import javax.management.remote.JMXServiceURL

class JmxHisto (host: String, port: Int) : AutoCloseable {

    private val connection: MBeanServerConnection
    private val jmxConnector: JMXConnector
    private val diagnosticCommandBean: ObjectName = ObjectName("com.sun.management:type=DiagnosticCommand")

    val gcClassHistogramParams = arrayOf<Any>(arrayOfNulls<String>(0))
    val gcClassHistogramSignature = arrayOf(Array<String>::class.java.name)

    init {
        val url = JMXServiceURL("service:jmx:rmi:///jndi/rmi://$host:$port/jmxrmi")
        jmxConnector = JMXConnectorFactory.connect(url, null)
        connection = jmxConnector.mBeanServerConnection
    }

    fun readHistogram(): String {
        val reply = connection.invoke(diagnosticCommandBean, "gcClassHistogram", gcClassHistogramParams, gcClassHistogramSignature)
        return reply as String
    }

    override fun close() {
        jmxConnector.close()
    }

}
