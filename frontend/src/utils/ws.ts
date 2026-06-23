import { useNotificationStore } from '@/stores/notification';
import { notificationsApi } from '@/api/notifications';

class NotificationSocket {
  private socket: WebSocket | null = null;
  private token = '';
  private reconnectTimer: number | null = null;
  private reconnectAttempts = 0;
  private manualClose = false;
  private connecting: Promise<void> | null = null;

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
    if (this.connecting) return;
    this.connecting = this.openWithTicket().finally(() => (this.connecting = null));
  }

  private async openWithTicket() {
    this.closeSocket();
    const { ticket } = await notificationsApi.wsTicket();
    if (this.manualClose || !this.token) return;
    const base = this.getBaseUrl();
    const url = `${base}/ws/notification?ticket=${encodeURIComponent(ticket)}`;
    this.socket = new WebSocket(url);

    this.socket.onopen = () => {
      this.reconnectAttempts = 0;
      useNotificationStore().fetchUnreadCount();
      useNotificationStore().fetchLatest();
    };

    this.socket.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data);
        if (payload.type === 'PING') {
          this.socket?.send(JSON.stringify({ type: 'PONG' }));
          return;
        }
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

    this.socket.onerror = () => this.socket?.close();
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
    const delay = Math.min(30000, 1200 * 2 ** this.reconnectAttempts) + Math.floor(Math.random() * 600);
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
