import os
import sys
import subprocess
import threading
import tkinter as tk
from tkinter import ttk, messagebox

# ADB Path auto-detection
ADB_CANDIDATES = [
    r"C:\Android\Sdk\platform-tools\adb.exe",
    os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"),
    "adb"
]

ADB_PATH = "adb"
for candidate in ADB_CANDIDATES:
    if os.path.exists(candidate):
        ADB_PATH = candidate
        break

# Keycodes
KEY_UP = 19
KEY_DOWN = 20
KEY_LEFT = 21
KEY_RIGHT = 22
KEY_OK = 23
KEY_BACK = 4
KEY_HOME = 3
KEY_MENU = 82
KEY_PLAY_PAUSE = 85
KEY_REWIND = 89
KEY_FAST_FORWARD = 90
KEY_CHANNEL_UP = 166
KEY_CHANNEL_DOWN = 167
KEY_VOL_UP = 24
KEY_VOL_DOWN = 25
KEY_MUTE = 164
KEY_POWER = 26

CONFIG_FILE = os.path.join(os.path.expanduser("~"), ".streamvault_remote_config")

def run_adb(args, timeout=6):
    try:
        cmd = [ADB_PATH] + args
        proc = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=timeout,
            creationflags=0x08000000 if os.name == "nt" else 0
        )
        return proc.stdout.strip()
    except Exception:
        return ""

class TVRemoteApp(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("StreamVault TV Remote")
        self.geometry("300x610")
        self.resizable(False, False)
        self.configure(bg="#0f172a")
        self.attributes("-topmost", True)

        self.current_device = ""
        self.available_devices = []
        self.saved_ip = self.load_saved_ip()

        self.style_config()
        self.create_widgets()
        self.bind_keyboard_events()

        # Start periodic device scanning
        self.refresh_devices()
        self.periodic_device_check()

    def load_saved_ip(self):
        try:
            if os.path.exists(CONFIG_FILE):
                with open(CONFIG_FILE, "r") as f:
                    return f.read().strip()
        except Exception:
            pass
        return "192.168.0."

    def save_ip(self, ip):
        try:
            with open(CONFIG_FILE, "w") as f:
                f.write(ip.strip())
        except Exception:
            pass

    def style_config(self):
        self.bg = "#0f172a"
        self.card_bg = "#1e293b"
        self.btn_bg = "#334155"
        self.btn_fg = "#f8fafc"
        self.btn_active = "#475569"
        self.accent_bg = "#2563eb"
        self.accent_active = "#1d4ed8"
        self.ch_bg = "#1e3a8a"
        self.ch_active = "#1d4ed8"
        self.font_main = ("Segoe UI", 9, "bold")
        self.font_dpad = ("Segoe UI", 12, "bold")
        self.font_title = ("Segoe UI", 10, "bold")
        self.font_small = ("Segoe UI", 8)

    def make_btn(self, parent, text, cmd, bg=None, fg=None, active_bg=None, font=None, width=5, height=1):
        bg = bg or self.btn_bg
        fg = fg or self.btn_fg
        active_bg = active_bg or self.btn_active
        font = font or self.font_main

        btn = tk.Button(
            parent,
            text=text,
            command=cmd,
            bg=bg,
            fg=fg,
            activebackground=active_bg,
            activeforeground="#ffffff",
            font=font,
            relief="flat",
            bd=0,
            cursor="hand2",
            padx=2,
            pady=3
        )
        return btn

    def send_key(self, code):
        def worker():
            target = self.current_device
            cmd = []
            if target:
                cmd = ["-s", target, "shell", "input", "keyevent", str(code)]
            else:
                cmd = ["shell", "input", "keyevent", str(code)]
            run_adb(cmd, timeout=3)
        threading.Thread(target=worker, daemon=True).start()

    def send_text(self, text):
        if not text:
            return
        def worker():
            target = self.current_device
            escaped = text.replace(" ", "%s")
            cmd = []
            if target:
                cmd = ["-s", target, "shell", "input", "text", escaped]
            else:
                cmd = ["shell", "input", "text", escaped]
            run_adb(cmd, timeout=5)
        threading.Thread(target=worker, daemon=True).start()

    def connect_ip(self):
        ip = self.ip_entry.get().strip()
        if not ip:
            return
        self.save_ip(ip)
        self.status_label.config(text=f"Connecting to {ip}...", fg="#f59e0b")

        def worker():
            target_port = ip if ":" in ip else f"{ip}:5555"
            output = run_adb(["connect", target_port], timeout=7)
            self.after(300, self.refresh_devices)
        threading.Thread(target=worker, daemon=True).start()

    def refresh_devices(self):
        def worker():
            output = run_adb(["devices"], timeout=5)
            devices = []
            for line in output.splitlines()[1:]:
                parts = line.strip().split()
                if len(parts) >= 2 and parts[1] == "device":
                    devices.append(parts[0])

            def update_ui():
                self.available_devices = devices
                if devices:
                    if self.current_device not in devices:
                        self.current_device = devices[0]
                    self.device_combo["values"] = devices
                    self.device_combo.set(self.current_device)
                    self.status_label.config(text=f"Connected: {self.current_device}", fg="#10b981")
                else:
                    self.current_device = ""
                    self.device_combo["values"] = ["No devices found"]
                    self.device_combo.set("No devices found")
                    self.status_label.config(text="No device connected (Start emulator / connect TV)", fg="#ef4444")
            self.after(0, update_ui)
        threading.Thread(target=worker, daemon=True).start()

    def on_device_selected(self, event=None):
        selected = self.device_combo.get()
        if selected and selected != "No devices found":
            self.current_device = selected
            self.status_label.config(text=f"Connected: {self.current_device}", fg="#10b981")

    def periodic_device_check(self):
        self.refresh_devices()
        self.after(8000, self.periodic_device_check)

    def create_widgets(self):
        # 1. Device Connection Bar
        top_frame = tk.Frame(self, bg=self.card_bg, padx=8, pady=6)
        top_frame.pack(fill="x", padx=10, pady=(8, 4))

        self.status_label = tk.Label(top_frame, text="Detecting devices...", bg=self.card_bg, fg="#94a3b8", font=self.font_small, anchor="w")
        self.status_label.pack(fill="x")

        dev_row = tk.Frame(top_frame, bg=self.card_bg, pady=2)
        dev_row.pack(fill="x")

        self.device_combo = ttk.Combobox(dev_row, values=["Scanning..."], state="readonly", width=18, font=("Segoe UI", 9))
        self.device_combo.pack(side="left", fill="x", expand=True, padx=(0, 4))
        self.device_combo.bind("<<ComboboxSelected>>", self.on_device_selected)

        btn_refresh = self.make_btn(dev_row, "🔄", self.refresh_devices, width=3)
        btn_refresh.pack(side="right")

        # IP Connect Row
        ip_row = tk.Frame(top_frame, bg=self.card_bg, pady=4)
        ip_row.pack(fill="x")

        self.ip_entry = tk.Entry(ip_row, bg="#0f172a", fg="#ffffff", insertbackground="#ffffff", font=("Segoe UI", 9), bd=1, relief="solid")
        self.ip_entry.insert(0, self.saved_ip)
        self.ip_entry.pack(side="left", fill="x", expand=True, padx=(0, 4))

        btn_conn = self.make_btn(ip_row, "Connect TV", self.connect_ip, bg="#0ea5e9", active_bg="#0284c7", width=10)
        btn_conn.pack(side="right")

        # 2. Power & Mute Bar
        pwr_frame = tk.Frame(self, bg=self.bg, pady=2)
        pwr_frame.pack(fill="x", padx=14)

        btn_power = self.make_btn(pwr_frame, "⏻ Power", lambda: self.send_key(KEY_POWER), bg="#dc2626", active_bg="#b91c1c", width=9)
        btn_power.pack(side="left", padx=2)

        btn_mute = self.make_btn(pwr_frame, "🔇 Mute", lambda: self.send_key(KEY_MUTE), width=9)
        btn_mute.pack(side="right", padx=2)

        # 3. D-Pad Frame
        dpad_frame = tk.Frame(self, bg=self.bg, pady=6)
        dpad_frame.pack()

        btn_up = self.make_btn(dpad_frame, "▲", lambda: self.send_key(KEY_UP), font=self.font_dpad, width=6, height=1)
        btn_up.grid(row=0, column=1, padx=4, pady=3)

        btn_left = self.make_btn(dpad_frame, "◀", lambda: self.send_key(KEY_LEFT), font=self.font_dpad, width=6, height=1)
        btn_left.grid(row=1, column=0, padx=4, pady=3)

        btn_ok = self.make_btn(dpad_frame, "OK", lambda: self.send_key(KEY_OK), bg=self.accent_bg, active_bg=self.accent_active, font=self.font_dpad, width=6, height=1)
        btn_ok.grid(row=1, column=1, padx=4, pady=3)

        btn_right = self.make_btn(dpad_frame, "▶", lambda: self.send_key(KEY_RIGHT), font=self.font_dpad, width=6, height=1)
        btn_right.grid(row=1, column=2, padx=4, pady=3)

        btn_down = self.make_btn(dpad_frame, "▼", lambda: self.send_key(KEY_DOWN), font=self.font_dpad, width=6, height=1)
        btn_down.grid(row=2, column=1, padx=4, pady=3)

        # 4. Nav Row (Back, Home, Menu)
        nav_frame = tk.Frame(self, bg=self.bg, pady=4)
        nav_frame.pack(fill="x", padx=14)

        btn_back = self.make_btn(nav_frame, "↩ Back", lambda: self.send_key(KEY_BACK), width=7)
        btn_back.pack(side="left", expand=True, padx=2)

        btn_home = self.make_btn(nav_frame, "⌂ Home", lambda: self.send_key(KEY_HOME), width=7)
        btn_home.pack(side="left", expand=True, padx=2)

        btn_menu = self.make_btn(nav_frame, "☰ Menu", lambda: self.send_key(KEY_MENU), width=7)
        btn_menu.pack(side="left", expand=True, padx=2)

        # 5. Channel Up/Down (Next/Prev Song & Channel)
        ch_frame = tk.Frame(self, bg=self.bg, pady=3)
        ch_frame.pack(fill="x", padx=14)

        btn_ch_up = self.make_btn(ch_frame, "CH ▲ (Next Song)", lambda: self.send_key(KEY_CHANNEL_UP), bg=self.ch_bg, active_bg=self.ch_active)
        btn_ch_up.pack(side="left", fill="x", expand=True, padx=2)

        btn_ch_down = self.make_btn(ch_frame, "CH ▼ (Prev Song)", lambda: self.send_key(KEY_CHANNEL_DOWN), bg=self.ch_bg, active_bg=self.ch_active)
        btn_ch_down.pack(side="left", fill="x", expand=True, padx=2)

        # 6. Media Row (Rewind 1m, Play/Pause, Forward 1m)
        media_frame = tk.Frame(self, bg=self.bg, pady=3)
        media_frame.pack(fill="x", padx=14)

        btn_rewind = self.make_btn(media_frame, "⏪ -1m", lambda: self.send_key(KEY_LEFT), width=7)
        btn_rewind.pack(side="left", expand=True, padx=2)

        btn_play = self.make_btn(media_frame, "⏯ Play", lambda: self.send_key(KEY_PLAY_PAUSE), bg=self.accent_bg, active_bg=self.accent_active, width=7)
        btn_play.pack(side="left", expand=True, padx=2)

        btn_forward = self.make_btn(media_frame, "+1m ⏩", lambda: self.send_key(KEY_RIGHT), width=7)
        btn_forward.pack(side="left", expand=True, padx=2)

        # 7. Volume Row
        vol_frame = tk.Frame(self, bg=self.bg, pady=3)
        vol_frame.pack(fill="x", padx=14)

        btn_voldown = self.make_btn(vol_frame, "Vol −", lambda: self.send_key(KEY_VOL_DOWN), width=10)
        btn_voldown.pack(side="left", expand=True, padx=2)

        btn_volup = self.make_btn(vol_frame, "Vol ＋", lambda: self.send_key(KEY_VOL_UP), width=10)
        btn_volup.pack(side="left", expand=True, padx=2)

        # 8. Keyboard Text Input to TV
        txt_frame = tk.Frame(self, bg=self.card_bg, padx=8, pady=5)
        txt_frame.pack(fill="x", padx=10, pady=(6, 8))

        lbl_txt = tk.Label(txt_frame, text="Type on TV (Search / URL):", bg=self.card_bg, fg="#94a3b8", font=self.font_small, anchor="w")
        lbl_txt.pack(fill="x")

        send_row = tk.Frame(txt_frame, bg=self.card_bg, pady=2)
        send_row.pack(fill="x")

        self.txt_entry = tk.Entry(send_row, bg="#0f172a", fg="#ffffff", insertbackground="#ffffff", font=("Segoe UI", 9), bd=1, relief="solid")
        self.txt_entry.pack(side="left", fill="x", expand=True, padx=(0, 4))
        self.txt_entry.bind("<Return>", lambda e: self.on_send_text_click())

        btn_send = self.make_btn(send_row, "Send", self.on_send_text_click, bg="#059669", active_bg="#047857", width=6)
        btn_send.pack(side="right")

    def on_send_text_click(self):
        txt = self.txt_entry.get().strip()
        if txt:
            self.send_text(txt)
            self.txt_entry.delete(0, tk.END)

    def bind_keyboard_events(self):
        # Ignore keyboard shortcuts when typing in entry fields
        def handle_key(code):
            focused = self.focus_get()
            if focused in [self.ip_entry, self.txt_entry]:
                return
            self.send_key(code)

        self.bind("<Up>", lambda e: handle_key(KEY_UP))
        self.bind("<Down>", lambda e: handle_key(KEY_DOWN))
        self.bind("<Left>", lambda e: handle_key(KEY_LEFT))
        self.bind("<Right>", lambda e: handle_key(KEY_RIGHT))
        self.bind("<Return>", lambda e: handle_key(KEY_OK))
        self.bind("<BackSpace>", lambda e: handle_key(KEY_BACK))
        self.bind("<Escape>", lambda e: handle_key(KEY_BACK))
        self.bind("<space>", lambda e: handle_key(KEY_PLAY_PAUSE))
        self.bind("<Prior>", lambda e: handle_key(KEY_CHANNEL_UP))
        self.bind("<Next>", lambda e: handle_key(KEY_CHANNEL_DOWN))

if __name__ == "__main__":
    app = TVRemoteApp()
    app.mainloop()
