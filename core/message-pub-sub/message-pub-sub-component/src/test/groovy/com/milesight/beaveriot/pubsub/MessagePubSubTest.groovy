package com.milesight.beaveriot.pubsub

import com.milesight.beaveriot.TestApplication
import com.milesight.beaveriot.pubsub.api.annotation.MessageListener
import com.milesight.beaveriot.pubsub.api.message.LocalUnicastMessage
import com.milesight.beaveriot.pubsub.api.message.RemoteBroadcastMessage
import org.spockframework.spring.SpringSpy
import org.spockframework.spring.UnwrapAopProxy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Component
import spock.lang.Specification

@SpringBootTest(classes = TestApplication)
class MessagePubSubTest extends Specification {

    @Autowired
    MessagePubSub messagePubSub

    @SpringSpy
    @UnwrapAopProxy
    TestListener testListener

    def "test publish"() {
        given:
        def localMessage = new TestLocalMessage(data: "Hello")
        def broadcastMessage = new TestBroadcastMessage(data: "World")

        when:
        messagePubSub.publish(localMessage)
        messagePubSub.publish(broadcastMessage)

        sleep(100) // Wait for the broadcastMessage to be processed in another thread

        then:
        1 * testListener.onLocalMessage(*_) >> { assert it[0].data == localMessage.data }
        1 * testListener.onBroadcastMessage(*_) >> { assert it[0].data == broadcastMessage.data }
    }

    @Component
    static class TestListener {

        @MessageListener
        void onLocalMessage(TestLocalMessage message) {
        }

        @MessageListener
        void onBroadcastMessage(TestBroadcastMessage message) {
        }

    }

    static class TestLocalMessage extends LocalUnicastMessage {
        String data
    }

    static class TestBroadcastMessage extends RemoteBroadcastMessage {
        String data
    }

}
