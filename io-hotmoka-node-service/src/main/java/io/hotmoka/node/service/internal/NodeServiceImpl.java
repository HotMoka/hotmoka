/*
Copyright 2021 Dinu Berinde and Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.node.service.internal;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.closeables.api.OnCloseHandler;
import io.hotmoka.network.requests.EventRequestModel;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.Subscription;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.messages.AddConstructorCallTransactionMessages;
import io.hotmoka.node.messages.AddConstructorCallTransactionResultMessages;
import io.hotmoka.node.messages.AddInstanceMethodCallTransactionMessages;
import io.hotmoka.node.messages.AddInstanceMethodCallTransactionResultMessages;
import io.hotmoka.node.messages.AddJarStoreTransactionMessages;
import io.hotmoka.node.messages.AddJarStoreTransactionResultMessages;
import io.hotmoka.node.messages.AddStaticMethodCallTransactionMessages;
import io.hotmoka.node.messages.AddStaticMethodCallTransactionResultMessages;
import io.hotmoka.node.messages.GetClassTagMessages;
import io.hotmoka.node.messages.GetClassTagResultMessages;
import io.hotmoka.node.messages.GetConsensusConfigMessages;
import io.hotmoka.node.messages.GetConsensusConfigResultMessages;
import io.hotmoka.node.messages.GetManifestMessages;
import io.hotmoka.node.messages.GetManifestResultMessages;
import io.hotmoka.node.messages.GetNodeInfoMessages;
import io.hotmoka.node.messages.GetNodeInfoResultMessages;
import io.hotmoka.node.messages.GetPolledResponseMessages;
import io.hotmoka.node.messages.GetPolledResponseResultMessages;
import io.hotmoka.node.messages.GetRequestMessages;
import io.hotmoka.node.messages.GetRequestResultMessages;
import io.hotmoka.node.messages.GetResponseMessages;
import io.hotmoka.node.messages.GetResponseResultMessages;
import io.hotmoka.node.messages.GetStateMessages;
import io.hotmoka.node.messages.GetStateResultMessages;
import io.hotmoka.node.messages.GetTakamakaCodeMessages;
import io.hotmoka.node.messages.GetTakamakaCodeResultMessages;
import io.hotmoka.node.messages.PostConstructorCallTransactionMessages;
import io.hotmoka.node.messages.PostConstructorCallTransactionResultMessages;
import io.hotmoka.node.messages.RunInstanceMethodCallTransactionMessages;
import io.hotmoka.node.messages.RunInstanceMethodCallTransactionResultMessages;
import io.hotmoka.node.messages.RunStaticMethodCallTransactionMessages;
import io.hotmoka.node.messages.RunStaticMethodCallTransactionResultMessages;
import io.hotmoka.node.messages.api.AddConstructorCallTransactionMessage;
import io.hotmoka.node.messages.api.AddInstanceMethodCallTransactionMessage;
import io.hotmoka.node.messages.api.AddJarStoreTransactionMessage;
import io.hotmoka.node.messages.api.AddStaticMethodCallTransactionMessage;
import io.hotmoka.node.messages.api.GetClassTagMessage;
import io.hotmoka.node.messages.api.GetConsensusConfigMessage;
import io.hotmoka.node.messages.api.GetManifestMessage;
import io.hotmoka.node.messages.api.GetNodeInfoMessage;
import io.hotmoka.node.messages.api.GetPolledResponseMessage;
import io.hotmoka.node.messages.api.GetRequestMessage;
import io.hotmoka.node.messages.api.GetResponseMessage;
import io.hotmoka.node.messages.api.GetStateMessage;
import io.hotmoka.node.messages.api.GetTakamakaCodeMessage;
import io.hotmoka.node.messages.api.PostConstructorCallTransactionMessage;
import io.hotmoka.node.messages.api.RunInstanceMethodCallTransactionMessage;
import io.hotmoka.node.messages.api.RunStaticMethodCallTransactionMessage;
import io.hotmoka.node.service.api.NodeService;
import io.hotmoka.node.service.api.NodeServiceConfig;
import io.hotmoka.node.service.internal.websockets.WebSocketsEventController;
import io.hotmoka.websockets.beans.ExceptionMessages;
import io.hotmoka.websockets.server.AbstractServerEndpoint;
import io.hotmoka.websockets.server.AbstractWebSocketServer;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * A simple web service that exposes some REST APIs to access an instance of a {@link io.hotmoka.node.api.Node}.
 */
public class NodeServiceImpl extends AbstractWebSocketServer implements NodeService {
	private final static Logger LOGGER = Logger.getLogger(NodeServiceImpl.class.getName());

	/**
	 * The node for which the service is created.
	 */
	private final Node node;

	private final ConfigurableApplicationContext context;

	/**
	 * The subscription to the events generated by the exposed node.
	 */
	private final Subscription eventSubscription;

	/**
	 * True if and only if this service has been closed already.
	 */
	private final AtomicBoolean isClosed = new AtomicBoolean();

	/**
	 * The prefix used in the log messages;
	 */
	private final String logPrefix;

	/**
	 * We need this intermediate definition since two instances of a method reference
	 * are not the same, nor equals.
	 */
	private final OnCloseHandler this_close = this::close;

	/**
	 * Yields an implementation of a network service that exposes an API to a given Hotmoka node.
	 * 
	 * @param config the configuration of the network
	 * @param node the Hotmoka node
	 * @throws DeploymentException if the service cannot be deployed
	 * @throws IOException if an I/O error occurs
	 */
    public NodeServiceImpl(NodeServiceConfig config, Node node) throws DeploymentException, IOException {
    	this.node = node;

    	// we disable Spring's logging otherwise it will interfere with Hotmoka's logging
		System.setProperty("org.springframework.boot.logging.LoggingSystem", "none");

		this.logPrefix = "node service(ws://localhost:" + config.getPort() + "): ";
		this.context = SpringApplication.run(Application.class, springArgumentsFor(config));
    	this.context.getBean(Application.class).setNode(node);
    	this.eventSubscription = node.subscribeToEvents(null, this::publishEvent);

    	// TODO: remove the +2 at the end
    	startContainer("", config.getPort() + 2,
   			GetNodeInfoEndpoint.config(this), GetConsensusConfigEndpoint.config(this), GetTakamakaCodeEndpoint.config(this),
   			GetManifestEndpoint.config(this), GetClassTagEndpoint.config(this), GetStateEndpoint.config(this),
   			GetRequestEndpoint.config(this), GetResponseEndpoint.config(this), GetPolledResponseEndpoint.config(this),
   			AddJarStoreTransactionEndpoint.config(this), AddConstructorCallTransactionEndpoint.config(this),
   			AddInstanceMethodCallTransactionEndpoint.config(this), AddStaticMethodCallTransactionEndpoint.config(this),
   			PostConstructorCallTransactionEndpoint.config(this),
   			RunInstanceMethodCallTransactionEndpoint.config(this), RunStaticMethodCallTransactionEndpoint.config(this)
   		);

    	// if the node gets closed, then this service will be closed as well
    	node.addOnCloseHandler(this_close);

    	LOGGER.info(logPrefix + "published");
    }

    @Override
    public void close() {
    	if (!isClosed.getAndSet(true)) {
    		node.removeOnCloseHandler(this_close);
    		stopContainer();
    		SpringApplication.exit(context);
        	eventSubscription.close();
			LOGGER.info(logPrefix + "closed");
		}
    }

    /**
	 * Sends an exception message to the given session.
	 * 
	 * @param session the session
	 * @param e the exception used to build the message
	 * @param id the identifier of the message to send
	 * @throws IOException if there was an I/O problem
	 */
	private void sendExceptionAsync(Session session, Exception e, String id) throws IOException {
		sendObjectAsync(session, ExceptionMessages.of(e, id));
	}

	protected void onGetNodeInfo(GetNodeInfoMessage message, Session session) {
		LOGGER.info(logPrefix + "received a " + GET_NODE_INFO_ENDPOINT + " request");

		try {
			try {
				sendObjectAsync(session, GetNodeInfoResultMessages.of(node.getNodeInfo(), message.getId()));
			}
			catch (TimeoutException | InterruptedException | NodeException e) {
				sendExceptionAsync(session, e, message.getId());
			}
		}
		catch (IOException e) {
			LOGGER.log(Level.SEVERE, logPrefix + "cannot send to session: it might be closed: " + e.getMessage());
		}
	};

	public static class GetNodeInfoEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			addMessageHandler(session, (GetNodeInfoMessage message) -> getServer().onGetNodeInfo(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, GetNodeInfoEndpoint.class, GET_NODE_INFO_ENDPOINT,
				GetNodeInfoMessages.Decoder.class, GetNodeInfoResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onGetConsensusConfig(GetConsensusConfigMessage message, Session session) {
		LOGGER.info(logPrefix + "received a " + GET_CONSENSUS_CONFIG_ENDPOINT + " request");

		try {
			try {
				sendObjectAsync(session, GetConsensusConfigResultMessages.of(node.getConsensusConfig(), message.getId()));
			}
			catch (TimeoutException | InterruptedException | NodeException e) {
				sendExceptionAsync(session, e, message.getId());
			}
		}
		catch (IOException e) {
			LOGGER.log(Level.SEVERE, logPrefix + "cannot send to session: it might be closed: " + e.getMessage());
		}
	};

	public static class GetConsensusConfigEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			addMessageHandler(session, (GetConsensusConfigMessage message) -> getServer().onGetConsensusConfig(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, GetConsensusConfigEndpoint.class, GET_CONSENSUS_CONFIG_ENDPOINT,
				GetConsensusConfigMessages.Decoder.class, GetConsensusConfigResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onGetTakamakaCode(GetTakamakaCodeMessage message, Session session) {
		LOGGER.info(logPrefix + "received a " + GET_TAKAMAKA_CODE_ENDPOINT + " request");

		try {
			try {
				sendObjectAsync(session, GetTakamakaCodeResultMessages.of(node.getTakamakaCode(), message.getId()));
			}
			catch (TimeoutException | InterruptedException | NodeException | NoSuchElementException e) { // TODO: remove NoSuchElement at the end
				sendExceptionAsync(session, e, message.getId());
			}
		}
		catch (IOException e) {
			LOGGER.log(Level.SEVERE, logPrefix + "cannot send to session: it might be closed: " + e.getMessage());
		}
	};

	public static class GetTakamakaCodeEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			addMessageHandler(session, (GetTakamakaCodeMessage message) -> getServer().onGetTakamakaCode(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, GetTakamakaCodeEndpoint.class, GET_TAKAMAKA_CODE_ENDPOINT,
				GetTakamakaCodeMessages.Decoder.class, GetTakamakaCodeResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onGetManifest(GetManifestMessage message, Session session) {
		LOGGER.info(logPrefix + "received a " + GET_MANIFEST_ENDPOINT + " request");

		try {
			try {
				sendObjectAsync(session, GetManifestResultMessages.of(node.getManifest(), message.getId()));
			}
			catch (TimeoutException | InterruptedException | NodeException | NoSuchElementException e) { // TODO: remove NoSuchElement at the end
				sendExceptionAsync(session, e, message.getId());
			}
		}
		catch (IOException e) {
			LOGGER.log(Level.SEVERE, logPrefix + "cannot send to session: it might be closed: " + e.getMessage());
		}
	};

	public static class GetManifestEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			addMessageHandler(session, (GetManifestMessage message) -> getServer().onGetManifest(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, GetManifestEndpoint.class, GET_MANIFEST_ENDPOINT,
				GetManifestMessages.Decoder.class, GetManifestResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onGetClassTag(GetClassTagMessage message, Session session) {
		LOGGER.info(logPrefix + "received a " + GET_CLASS_TAG_ENDPOINT + " request");

		try {
			try {
				sendObjectAsync(session, GetClassTagResultMessages.of(node.getClassTag(message.getReference()), message.getId()));
			}
			catch (TimeoutException | InterruptedException | NodeException | NoSuchElementException e) {
				sendExceptionAsync(session, e, message.getId());
			}
		}
		catch (IOException e) {
			LOGGER.log(Level.SEVERE, logPrefix + "cannot send to session: it might be closed: " + e.getMessage());
		}
	};

	public static class GetClassTagEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			addMessageHandler(session, (GetClassTagMessage message) -> getServer().onGetClassTag(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, GetClassTagEndpoint.class, GET_CLASS_TAG_ENDPOINT,
				GetClassTagMessages.Decoder.class, GetClassTagResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onGetState(GetStateMessage message, Session session) {
		LOGGER.info(logPrefix + "received a " + GET_STATE_ENDPOINT + " request");

		try {
			try {
				sendObjectAsync(session, GetStateResultMessages.of(node.getState(message.getReference()), message.getId()));
			}
			catch (TimeoutException | InterruptedException | NodeException | NoSuchElementException e) {
				sendExceptionAsync(session, e, message.getId());
			}
		}
		catch (IOException e) {
			LOGGER.log(Level.SEVERE, logPrefix + "cannot send to session: it might be closed: " + e.getMessage());
		}
	};

	public static class GetStateEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			addMessageHandler(session, (GetStateMessage message) -> getServer().onGetState(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, GetStateEndpoint.class, GET_STATE_ENDPOINT,
				GetStateMessages.Decoder.class, GetStateResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onGetRequest(GetRequestMessage message, Session session) {
		LOGGER.info(logPrefix + "received a " + GET_REQUEST_ENDPOINT + " request");

		try {
			try {
				sendObjectAsync(session, GetRequestResultMessages.of(node.getRequest(message.getReference()), message.getId()));
			}
			catch (TimeoutException | InterruptedException | NodeException | NoSuchElementException e) {
				sendExceptionAsync(session, e, message.getId());
			}
		}
		catch (IOException e) {
			LOGGER.log(Level.SEVERE, logPrefix + "cannot send to session: it might be closed: " + e.getMessage());
		}
	};

	public static class GetRequestEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			addMessageHandler(session, (GetRequestMessage message) -> getServer().onGetRequest(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, GetRequestEndpoint.class, GET_REQUEST_ENDPOINT,
				GetRequestMessages.Decoder.class, GetRequestResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onGetResponse(GetResponseMessage message, Session session) {
		LOGGER.info(logPrefix + "received a " + GET_RESPONSE_ENDPOINT + " request");

		try {
			try {
				sendObjectAsync(session, GetResponseResultMessages.of(node.getResponse(message.getReference()), message.getId()));
			}
			catch (TimeoutException | InterruptedException | NodeException | TransactionRejectedException | NoSuchElementException e) {
				sendExceptionAsync(session, e, message.getId());
			}
		}
		catch (IOException e) {
			LOGGER.log(Level.SEVERE, logPrefix + "cannot send to session: it might be closed: " + e.getMessage());
		}
	};

	public static class GetResponseEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			addMessageHandler(session, (GetResponseMessage message) -> getServer().onGetResponse(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, GetResponseEndpoint.class, GET_RESPONSE_ENDPOINT,
				GetResponseMessages.Decoder.class, GetResponseResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onGetPolledResponse(GetPolledResponseMessage message, Session session) {
		LOGGER.info(logPrefix + "received a " + GET_POLLED_RESPONSE_ENDPOINT + " request");

		try {
			try {
				sendObjectAsync(session, GetPolledResponseResultMessages.of(node.getPolledResponse(message.getReference()), message.getId()));
			}
			catch (TimeoutException | InterruptedException | NodeException | TransactionRejectedException e) {
				sendExceptionAsync(session, e, message.getId());
			}
		}
		catch (IOException e) {
			LOGGER.log(Level.SEVERE, logPrefix + "cannot send to session: it might be closed: " + e.getMessage());
		}
	};

	public static class GetPolledResponseEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			addMessageHandler(session, (GetPolledResponseMessage message) -> getServer().onGetPolledResponse(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, GetPolledResponseEndpoint.class, GET_POLLED_RESPONSE_ENDPOINT,
				GetPolledResponseMessages.Decoder.class, GetPolledResponseResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onRunInstanceMethodCallTransaction(RunInstanceMethodCallTransactionMessage message, Session session) {
		LOGGER.info(logPrefix + "received a " + RUN_INSTANCE_METHOD_CALL_TRANSACTION_ENDPOINT + " request");

		try {
			try {
				sendObjectAsync(session, RunInstanceMethodCallTransactionResultMessages.of(Optional.ofNullable(node.runInstanceMethodCallTransaction(message.getRequest())), message.getId()));
			}
			catch (TimeoutException | InterruptedException | NodeException | TransactionRejectedException | TransactionException | CodeExecutionException e) {
				sendExceptionAsync(session, e, message.getId());
			}
		}
		catch (IOException e) {
			LOGGER.log(Level.SEVERE, logPrefix + "cannot send to session: it might be closed: " + e.getMessage());
		}
	};

	public static class RunInstanceMethodCallTransactionEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			addMessageHandler(session, (RunInstanceMethodCallTransactionMessage message) -> getServer().onRunInstanceMethodCallTransaction(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, RunInstanceMethodCallTransactionEndpoint.class, RUN_INSTANCE_METHOD_CALL_TRANSACTION_ENDPOINT,
				RunInstanceMethodCallTransactionMessages.Decoder.class, RunInstanceMethodCallTransactionResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onRunStaticMethodCallTransaction(RunStaticMethodCallTransactionMessage message, Session session) {
		LOGGER.info(logPrefix + "received a " + RUN_STATIC_METHOD_CALL_TRANSACTION_ENDPOINT + " request");

		try {
			try {
				sendObjectAsync(session, RunStaticMethodCallTransactionResultMessages.of(Optional.ofNullable(node.runStaticMethodCallTransaction(message.getRequest())), message.getId()));
			}
			catch (TimeoutException | InterruptedException | NodeException | TransactionRejectedException | TransactionException | CodeExecutionException e) {
				sendExceptionAsync(session, e, message.getId());
			}
		}
		catch (IOException e) {
			LOGGER.log(Level.SEVERE, logPrefix + "cannot send to session: it might be closed: " + e.getMessage());
		}
	};

	public static class RunStaticMethodCallTransactionEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			addMessageHandler(session, (RunStaticMethodCallTransactionMessage message) -> getServer().onRunStaticMethodCallTransaction(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, RunStaticMethodCallTransactionEndpoint.class, RUN_STATIC_METHOD_CALL_TRANSACTION_ENDPOINT,
				RunStaticMethodCallTransactionMessages.Decoder.class, RunStaticMethodCallTransactionResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onAddInstanceMethodCallTransaction(AddInstanceMethodCallTransactionMessage message, Session session) {
		LOGGER.info(logPrefix + "received an " + ADD_INSTANCE_METHOD_CALL_TRANSACTION_ENDPOINT + " request");

		try {
			try {
				sendObjectAsync(session, AddInstanceMethodCallTransactionResultMessages.of(Optional.ofNullable(node.addInstanceMethodCallTransaction(message.getRequest())), message.getId()));
			}
			catch (TimeoutException | InterruptedException | NodeException | TransactionRejectedException | TransactionException | CodeExecutionException e) {
				sendExceptionAsync(session, e, message.getId());
			}
		}
		catch (IOException e) {
			LOGGER.log(Level.SEVERE, logPrefix + "cannot send to session: it might be closed: " + e.getMessage());
		}
	};

	public static class AddInstanceMethodCallTransactionEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			addMessageHandler(session, (AddInstanceMethodCallTransactionMessage message) -> getServer().onAddInstanceMethodCallTransaction(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, AddInstanceMethodCallTransactionEndpoint.class, ADD_INSTANCE_METHOD_CALL_TRANSACTION_ENDPOINT,
				AddInstanceMethodCallTransactionMessages.Decoder.class, AddInstanceMethodCallTransactionResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onAddStaticMethodCallTransaction(AddStaticMethodCallTransactionMessage message, Session session) {
		LOGGER.info(logPrefix + "received an " + ADD_STATIC_METHOD_CALL_TRANSACTION_ENDPOINT + " request");

		try {
			try {
				sendObjectAsync(session, AddStaticMethodCallTransactionResultMessages.of(Optional.ofNullable(node.addStaticMethodCallTransaction(message.getRequest())), message.getId()));
			}
			catch (TimeoutException | InterruptedException | NodeException | TransactionRejectedException | TransactionException | CodeExecutionException e) {
				sendExceptionAsync(session, e, message.getId());
			}
		}
		catch (IOException e) {
			LOGGER.log(Level.SEVERE, logPrefix + "cannot send to session: it might be closed: " + e.getMessage());
		}
	};

	public static class AddStaticMethodCallTransactionEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			addMessageHandler(session, (AddStaticMethodCallTransactionMessage message) -> getServer().onAddStaticMethodCallTransaction(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, AddStaticMethodCallTransactionEndpoint.class, ADD_STATIC_METHOD_CALL_TRANSACTION_ENDPOINT,
				AddStaticMethodCallTransactionMessages.Decoder.class, AddStaticMethodCallTransactionResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onAddConstructorCallTransaction(AddConstructorCallTransactionMessage message, Session session) {
		LOGGER.info(logPrefix + "received an " + ADD_CONSTRUCTOR_CALL_TRANSACTION_ENDPOINT + " request");

		try {
			try {
				sendObjectAsync(session, AddConstructorCallTransactionResultMessages.of(node.addConstructorCallTransaction(message.getRequest()), message.getId()));
			}
			catch (TimeoutException | InterruptedException | NodeException | TransactionRejectedException | TransactionException | CodeExecutionException e) {
				sendExceptionAsync(session, e, message.getId());
			}
		}
		catch (IOException e) {
			LOGGER.log(Level.SEVERE, logPrefix + "cannot send to session: it might be closed: " + e.getMessage());
		}
	};

	public static class AddConstructorCallTransactionEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			addMessageHandler(session, (AddConstructorCallTransactionMessage message) -> getServer().onAddConstructorCallTransaction(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, AddConstructorCallTransactionEndpoint.class, ADD_CONSTRUCTOR_CALL_TRANSACTION_ENDPOINT,
				AddConstructorCallTransactionMessages.Decoder.class, AddConstructorCallTransactionResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onAddJarStoreTransaction(AddJarStoreTransactionMessage message, Session session) {
		LOGGER.info(logPrefix + "received an " + ADD_JAR_STORE_TRANSACTION_ENDPOINT + " request");

		try {
			try {
				sendObjectAsync(session, AddJarStoreTransactionResultMessages.of(node.addJarStoreTransaction(message.getRequest()), message.getId()));
			}
			catch (TimeoutException | InterruptedException | NodeException | TransactionRejectedException | TransactionException e) {
				sendExceptionAsync(session, e, message.getId());
			}
		}
		catch (IOException e) {
			LOGGER.log(Level.SEVERE, logPrefix + "cannot send to session: it might be closed: " + e.getMessage());
		}
	};

	public static class AddJarStoreTransactionEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			addMessageHandler(session, (AddJarStoreTransactionMessage message) -> getServer().onAddJarStoreTransaction(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, AddJarStoreTransactionEndpoint.class, ADD_JAR_STORE_TRANSACTION_ENDPOINT,
				AddJarStoreTransactionMessages.Decoder.class, AddJarStoreTransactionResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onPostConstructorCallTransaction(PostConstructorCallTransactionMessage message, Session session) {
		LOGGER.info(logPrefix + "received a " + POST_CONSTRUCTOR_CALL_TRANSACTION_ENDPOINT + " request");

		try {
			try {
				sendObjectAsync(session, PostConstructorCallTransactionResultMessages.of(node.postConstructorCallTransaction(message.getRequest()).getReferenceOfRequest(), message.getId()));
			}
			catch (TimeoutException | InterruptedException | NodeException | TransactionRejectedException e) {
				sendExceptionAsync(session, e, message.getId());
			}
		}
		catch (IOException e) {
			LOGGER.log(Level.SEVERE, logPrefix + "cannot send to session: it might be closed: " + e.getMessage());
		}
	};

	public static class PostConstructorCallTransactionEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			addMessageHandler(session, (PostConstructorCallTransactionMessage message) -> getServer().onPostConstructorCallTransaction(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, PostConstructorCallTransactionEndpoint.class, POST_CONSTRUCTOR_CALL_TRANSACTION_ENDPOINT,
				PostConstructorCallTransactionMessages.Decoder.class, PostConstructorCallTransactionResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	/**
     * Builds, from the configuration, the array of arguments required by Spring in order to start the application.
     * 
     * @param config the configuration
     * @return the array of arguments required by Spring
     */
    private static String[] springArgumentsFor(NodeServiceConfig config) {
    	return new String[] {
   			"--server.port=" + config.getPort(),
   			"--spring.main.banner-mode=" + Banner.Mode.OFF
    	};
    }

    private void publishEvent(StorageReference creator, StorageReference event) {
		WebSocketsEventController controller = this.context.getBean(WebSocketsEventController.class);
		controller.addEvent(new EventRequestModel(creator, event));
    }
}