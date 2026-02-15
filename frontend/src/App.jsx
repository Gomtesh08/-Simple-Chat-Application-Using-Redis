import { useEffect, useRef, useState } from "react";

const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080/api/chatapp";

function App() {
  const [roomName, setRoomName] = useState("general");
  const [participant, setParticipant] = useState("guest_user");
  const [message, setMessage] = useState("");
  const [historyLimit, setHistoryLimit] = useState(10);
  const [messages, setMessages] = useState([]);
  const [status, setStatus] = useState("");
  const eventSourceRef = useRef(null);

  const createRoom = async () => {
    const response = await fetch(`${API_BASE}/chatrooms`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ roomName })
    });
    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.message || "Failed to create room");
    }
    setStatus(data.message);
  };

  const joinRoom = async () => {
    const response = await fetch(`${API_BASE}/chatrooms/${roomName}/join`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ participant })
    });
    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.message || "Failed to join room");
    }
    setStatus(data.message);
  };

  const sendMessage = async () => {
    if (!message.trim()) {
      return;
    }
    const response = await fetch(`${API_BASE}/chatrooms/${roomName}/messages`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ participant, message })
    });
    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.message || "Failed to send message");
    }
    setMessage("");
    setStatus(data.message);
  };

  const loadHistory = async () => {
    const response = await fetch(
      `${API_BASE}/chatrooms/${roomName}/messages?limit=${historyLimit}`
    );
    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.message || "Failed to load history");
    }
    setMessages(data.messages || []);
  };

  const connectStream = () => {
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
    }

    const streamUrl = `${API_BASE}/chatrooms/${roomName}/stream`;
    const eventSource = new EventSource(streamUrl);
    eventSource.addEventListener("message", (event) => {
      const nextMessage = JSON.parse(event.data);
      setMessages((previous) => {
        if (previous.length > 0 && previous[previous.length - 1].timestamp === nextMessage.timestamp) {
          return previous;
        }
        return [...previous, nextMessage];
      });
    });
    eventSource.onerror = () => {
      setStatus("Stream disconnected.");
    };
    eventSourceRef.current = eventSource;
    setStatus(`Subscribed to room '${roomName}' stream.`);
  };

  useEffect(() => {
    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }
    };
  }, []);

  const execute = async (action) => {
    try {
      await action();
    } catch (error) {
      setStatus(error.message);
    }
  };

  return (
    <div className="page">
      <div className="panel">
        <h1>Simple Chat App</h1>
        <p className="status">{status || "No activity yet."}</p>

        <div className="row">
          <label>Room</label>
          <input value={roomName} onChange={(e) => setRoomName(e.target.value)} />
        </div>

        <div className="row">
          <label>Participant</label>
          <input value={participant} onChange={(e) => setParticipant(e.target.value)} />
        </div>

        <div className="buttons">
          <button onClick={() => execute(createRoom)}>Create Room</button>
          <button onClick={() => execute(joinRoom)}>Join Room</button>
          <button onClick={() => execute(connectStream)}>Connect Stream</button>
          <button onClick={() => execute(loadHistory)}>Load History</button>
        </div>

        <div className="row">
          <label>History Limit</label>
          <input
            type="number"
            min="1"
            value={historyLimit}
            onChange={(e) => setHistoryLimit(Number(e.target.value || 1))}
          />
        </div>

        <div className="row">
          <label>Message</label>
          <input
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && execute(sendMessage)}
          />
        </div>
        <div className="buttons">
          <button onClick={() => execute(sendMessage)}>Send</button>
        </div>
      </div>

      <div className="panel">
        <h2>Messages</h2>
        <div className="messages">
          {messages.length === 0 && <p>No messages loaded.</p>}
          {messages.map((msg, index) => (
            <div className="message" key={`${msg.timestamp}-${index}`}>
              <div>
                <strong>{msg.participant}</strong>: {msg.message}
              </div>
              <small>{msg.timestamp}</small>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

export default App;
