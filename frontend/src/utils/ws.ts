import { useNotificationStore } from '@/stores/notification';

class NotificationSocket {
  private socket: WebSocket | null = null;
  private token = '';
  private reconnectTimer: number | null = null;
  private reconnectAttempts = 0;
  private manualClose = false;

  connect(token: string) {
    if (!token) return;
    if (
      this.socket &&
      this.token === token &&
      (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING)
    ) {
      return;
    }

    this.token = token;
    this.manualClose = false;
    this.closeSocket();

    const base = this.getBaseUrl();
    const url = `${base}/ws/notification?token=${encodeURIComponent(token)}`;
    this.socket = new WebSocket(url);

    this.socket.onopen = () => {
      this.reconnectAttempts = 0;
    };

    this.socket.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data);
        useNotificationStore().receiveRealtime(payload);
      } catch {
        useNotificationStore().receiveRealtime({
          title: '新的系统通知',
          content: String(event.data || '')
        });
      }
    };

    this.socket.onclose = () => {
      if (!this.manualClose && this.token) {
        this.scheduleReconnect();
      }
    };
  }

  disconnect() {
    this.manualClose = true;
    this.token = '';
    if (this.reconnectTimer) {
      window.clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    this.closeSocket();
  }

  private scheduleReconnect() {
    if (this.reconnectTimer) return;
    const delay = Math.min(30000, 1200 * 2 ** this.reconnectAttempts);
    this.reconnectAttempts += 1;
    this.reconnectTimer = window.setTimeout(() => {
      this.reconnectTimer = null;
      this.connect(this.token);
    }, delay);
  }

  private closeSocket() {
    if (this.socket) {
      this.socket.onclose = null;
      this.socket.close();
      this.socket = null;
    }
  }

  private getBaseUrl() {
    const configured = import.meta.env.VITE_WS_BASE_URL as string | undefined;
    if (configured) return configured.replace(/\/$/, '');
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    return `${protocol}://${window.location.host}`;
  }
}

export const notificationSocket = new NotificationSocket();
