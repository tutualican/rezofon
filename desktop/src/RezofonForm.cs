using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Text;
using System.IO;
using System.Media;
using System.Text;
using System.Windows.Forms;

namespace Rezofon
{
    internal class RezofonForm : Form
    {
        [System.Runtime.InteropServices.DllImport("dwmapi.dll")]
        static extern int DwmSetWindowAttribute(IntPtr hwnd, int attr, ref int value, int size);

        readonly Palette p = new Palette();
        readonly State st = new State();
        readonly ApoBridge apo = new ApoBridge();

        string screen = "home";
        readonly List<Hit> hits = new List<Hit>();
        readonly Dictionary<string, float> scrolls = new Dictionary<string, float>();

        string dragAction = null;
        int dragIndex = -1;
        float dragStartY;
        float dragStartValue;

        readonly Spring press = new Spring(1f);
        string pressedAction = null;
        Point mouse = new Point(-1, -1);
        float wavePhase;
        float toastTimer;
        string toast;
        bool eapoApplied;
        SoundPlayer guardPlayer;

        Timer anim;

        public RezofonForm()
        {
            Text = "Rezofon — JAM+ Ses Atölyesi";
            FormBorderStyle = FormBorderStyle.Sizable;
            StartPosition = FormStartPosition.CenterScreen;
            ClientSize = new Size(1240, 900);
            MinimumSize = new Size(1080, 760);
            DoubleBuffered = true;
            BackColor = p.Bg;
            KeyPreview = true;

            st.Load();
            eapoApplied = apo.HasBlock();
            Fonts.Load(AppDomain.CurrentDomain.BaseDirectory);

            string[] args = Environment.GetCommandLineArgs();
            if (args.Length > 1)
            {
                string want = args[1].ToLowerInvariant();
                if (want == "eq" || want == "bass" || want == "device" || want == "workshop")
                    screen = want;
            }

            anim = new Timer();
            anim.Interval = 16;
            anim.Tick += (s, e) =>
            {
                bool alive = press.Step(0.016f);
                wavePhase += 0.045f;
                if (wavePhase > 6.283f) wavePhase -= 6.283f;
                if (toastTimer > 0)
                {
                    toastTimer -= 0.016f;
                    alive = true;
                }
                if (alive) Invalidate();
            };
            anim.Start();

            MouseWheel += OnWheel;
            KeyDown += (s, e) => { if (e.KeyCode == Keys.Escape) { if (screen != "home") { screen = "home"; Invalidate(); } } };
        }

        protected override void OnLoad(EventArgs e)
        {
            base.OnLoad(e);
            try
            {
                int dark = 1;
                DwmSetWindowAttribute(Handle, 20, ref dark, sizeof(int));
            }
            catch { }
            // First run: show the factory ballanced curve.
            if (st.ActivePreset != null && st.ActivePreset.Length > 0 && Math.Abs(st.Gains[0]) < 0.01f)
            {
                List<Preset> all = Catalog.Presets();
                for (int i = 0; i < all.Count; i++)
                    if (all[i].Id == st.ActivePreset) st.ApplyPreset(all[i]);
            }
        }

        protected override void OnFormClosed(FormClosedEventArgs e)
        {
            st.Save();
            StopGuard();
            anim.Stop();
            base.OnFormClosed(e);
        }

        void ShowToast(string msg)
        {
            toast = msg;
            toastTimer = 2.8f;
            Invalidate();
        }

        // ------------------------------------------------------------------
        // Rendering
        // ------------------------------------------------------------------
        protected override void OnPaint(PaintEventArgs e)
        {
            Graphics g = e.Graphics;
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.TextRenderingHint = TextRenderingHint.ClearTypeGridFit;
            hits.Clear();

            DrawBackdrop(g);
            RectangleF rail = new RectangleF(18, 18, 88, ClientSize.Height - 36);
            RectangleF content = new RectangleF(126, 18, ClientSize.Width - 144, ClientSize.Height - 36);

            DrawRail(g, rail);

            g.SetClip(content);
            float scroll = scrolls.ContainsKey(screen) ? scrolls[screen] : 0f;
            g.TranslateTransform(0, -scroll);

            if (screen == "home") DrawHome(g, content);
            else if (screen == "eq") DrawEq(g, content);
            else if (screen == "bass") DrawBass(g, content);
            else if (screen == "device") DrawDevice(g, content);
            else if (screen == "workshop") DrawWorkshop(g, content);

            g.ResetTransform();
            g.ResetClip();

            if (toastTimer > 0 && toast != null)
            {
                SizeF m = Draw.Measure(g, toast, Fonts.B(12.5f, FontStyle.Bold));
                RectangleF box = new RectangleF(
                    content.X + (content.Width - m.Width) / 2f - 18f,
                    content.Y + 8f, m.Width + 36f, 38f);
                int alpha = (int)(Math.Min(1f, toastTimer / 0.35f) * 255);
                using (GraphicsPath path = Draw.Round(box, 19f))
                using (SolidBrush b = new SolidBrush(Color.FromArgb(alpha, p.Text)))
                {
                    g.FillPath(b, path);
                }
                Draw.Text(g, toast, Fonts.B(12.5f, FontStyle.Bold),
                    Color.FromArgb(alpha, p.Bg), box.X + 18f, box.Y + 11f);
            }
        }

        void DrawBackdrop(Graphics g)
        {
            using (LinearGradientBrush bg = new LinearGradientBrush(ClientRectangle, p.Bg, Color.FromArgb(11, 13, 18), 90f))
                g.FillRectangle(bg, ClientRectangle);

            using (GraphicsPath path = new GraphicsPath())
            {
                path.AddEllipse(ClientSize.Width * 0.05f - 620, -520, 1240, 1240);
                using (PathGradientBrush pb = new PathGradientBrush(path))
                {
                    pb.CenterColor = Color.FromArgb(26, p.Lime);
                    pb.SurroundColors = new Color[] { Color.Transparent };
                    g.FillPath(pb, path);
                }
            }
            using (GraphicsPath path = new GraphicsPath())
            {
                path.AddEllipse(ClientSize.Width * 0.9f - 500, ClientSize.Height * 0.75f - 500, 1000, 1000);
                using (PathGradientBrush pb = new PathGradientBrush(path))
                {
                    pb.CenterColor = Color.FromArgb(18, p.Amber);
                    pb.SurroundColors = new Color[] { Color.Transparent };
                    g.FillPath(pb, path);
                }
            }
        }

        void DrawRail(Graphics g, RectangleF rail)
        {
            using (GraphicsPath path = Draw.Round(rail, 30f))
            using (SolidBrush b = new SolidBrush(Color.FromArgb(232, p.Surface)))
                g.FillPath(b, path);
            using (GraphicsPath path = Draw.Round(rail, 30f))
            using (Pen pen = new Pen(p.Outline, 1f))
                g.DrawPath(pen, path);

            // logo mark
            RectangleF mark = new RectangleF(rail.X + 26f, rail.Y + 24f, 36f, 36f);
            for (int i = 0; i < 4; i++)
            {
                float h = i == 1 || i == 2 ? 30f : 16f;
                RectangleF bar = new RectangleF(mark.X + i * 9f, mark.Y + 18f - h / 2f, 5.5f, h);
                using (GraphicsPath bp = Draw.Round(bar, 2.7f))
                using (SolidBrush b = new SolidBrush(i == 2 ? p.Amber : p.Lime))
                    g.FillPath(b, bp);
            }

            string[] keys = { "home", "eq", "bass", "device", "workshop" };
            string[] labels = { "ANA", "EQ", "BAS", "CİHAZ", "ATÖLYE" };
            float y0 = rail.Y + 108f;
            for (int i = 0; i < keys.Length; i++)
            {
                RectangleF item = new RectangleF(rail.X + 14f, y0 + i * 76f, rail.Width - 28f, 64f);
                bool sel = screen == keys[i];
                bool hover = item.Contains(mouse);
                if (sel)
                {
                    using (GraphicsPath path = Draw.Round(item, 20f))
                    using (SolidBrush b = new SolidBrush(p.Lime))
                        g.FillPath(b, path);
                }
                else if (hover)
                {
                    using (GraphicsPath path = Draw.Round(item, 20f))
                    using (SolidBrush b = new SolidBrush(p.SurfaceAlt))
                        g.FillPath(b, path);
                }
                Color fg = sel ? p.Ink : (hover ? p.Text : p.Dim);
                Draw.Icon(g, keys[i], new RectangleF(item.X + item.Width / 2f - 12f, item.Y + 12f, 24f, 24f), fg);
                SizeF m = Draw.Measure(g, labels[i], Fonts.B(8.6f, FontStyle.Bold));
                Draw.Text(g, labels[i], Fonts.B(8.6f, FontStyle.Bold), fg,
                    item.X + (item.Width - m.Width) / 2f, item.Y + 40f);

                hits.Add(new Hit(item, "nav", i));
            }

            // guard badge
            RectangleF badge = new RectangleF(rail.X + 14f, rail.Bottom - 78f, rail.Width - 28f, 58f);
            bool on = st.Guard;
            using (GraphicsPath path = Draw.Round(badge, 18f))
            using (SolidBrush b = new SolidBrush(on ? Draw.Alpha(p.Cyan, 30) : p.SurfaceAlt))
                g.FillPath(b, path);
            Draw.Icon(g, "timer", new RectangleF(badge.X + badge.Width / 2f - 11f, badge.Y + 9f, 22f, 22f),
                on ? p.Cyan : p.Dim);
            SizeF bm = Draw.Measure(g, on ? "AÇIK" : "KAPALI", Fonts.B(8.4f, FontStyle.Bold));
            Draw.Text(g, on ? "AÇIK" : "KAPALI", Fonts.B(8.4f, FontStyle.Bold), on ? p.Cyan : p.Dim,
                badge.X + (badge.Width - bm.Width) / 2f, badge.Y + 34f);
            hits.Add(new Hit(badge, "toggle_guard"));
        }

        // ------------------------------------------------------------------
        // Shared bits
        // ------------------------------------------------------------------
        void Header(Graphics g, RectangleF c, string kicker, string title, string subtitle,
            string action, string actionIcon, string actionLabel)
        {
            Draw.Text(g, kicker, Fonts.B(10.5f, FontStyle.Bold), p.Lime, c.X, c.Y + 4f);
            Draw.Text(g, title, Fonts.D(34f, FontStyle.Bold), p.Text, c.X - 2f, c.Y + 22f);
            Draw.Text(g, subtitle, Fonts.B(12f, FontStyle.Regular), p.Dim, c.X, c.Y + 66f);

            if (action != null)
            {
                SizeF m = Draw.Measure(g, actionLabel, Fonts.B(10.5f, FontStyle.Bold));
                RectangleF btn = new RectangleF(c.Right - m.Width - 44f, c.Y + 18f, m.Width + 34f, 42f);
                bool hover = btn.Contains(mouse);
                using (GraphicsPath path = Draw.Round(btn, 14f))
                using (SolidBrush b = new SolidBrush(hover ? p.SurfaceAlt : Draw.Alpha(p.SurfaceAlt, 170)))
                    g.FillPath(b, path);
                using (GraphicsPath path = Draw.Round(btn, 14f))
                using (Pen pen = new Pen(p.Outline, 1f))
                    g.DrawPath(pen, path);
                Draw.Icon(g, actionIcon, new RectangleF(btn.X + 12f, btn.Y + 12f, 18f, 18f), p.Text);
                Draw.Text(g, actionLabel, Fonts.B(10.5f, FontStyle.Bold), p.Text, btn.X + 36f, btn.Y + 13f);
                hits.Add(new Hit(btn, action));
            }
        }

        void Kicker(Graphics g, string text, float x, float y)
        {
            Draw.Text(g, text.ToUpperInvariant(), Fonts.B(9.2f, FontStyle.Bold), p.Dim, x, y);
        }

        void Chip(Graphics g, RectangleF r, string text, bool selected, string action, int index)
        {
            bool hover = r.Contains(mouse);
            using (GraphicsPath path = Draw.Round(r, r.Height / 2f))
            using (SolidBrush b = new SolidBrush(selected ? p.Lime : (hover ? p.SurfaceAlt : Draw.Alpha(p.SurfaceAlt, 200))))
                g.FillPath(b, path);
            if (!selected)
            {
                using (GraphicsPath path = Draw.Round(r, r.Height / 2f))
                using (Pen pen = new Pen(p.Outline, 1f))
                    g.DrawPath(pen, path);
            }
            SizeF m = Draw.Measure(g, text, Fonts.B(10.8f, FontStyle.Bold));
            Draw.Text(g, text, Fonts.B(10.8f, FontStyle.Bold), selected ? p.Ink : p.Text,
                r.X + (r.Width - m.Width) / 2f, r.Y + (r.Height - m.Height) / 2f);
            hits.Add(new Hit(r, action, index));
        }

        void Toggle(Graphics g, RectangleF r, bool on, string action)
        {
            using (GraphicsPath path = Draw.Round(r, r.Height / 2f))
            using (SolidBrush b = new SolidBrush(on ? p.Lime : p.Sunken))
                g.FillPath(b, path);
            if (!on)
            {
                using (GraphicsPath path = Draw.Round(r, r.Height / 2f))
                using (Pen pen = new Pen(p.Outline, 1f))
                    g.DrawPath(pen, path);
            }
            float d = r.Height - 8f;
            float x = on ? r.Right - d - 4f : r.X + 4f;
            using (SolidBrush b = new SolidBrush(on ? p.Ink : p.Dim))
                g.FillEllipse(b, x, r.Y + 4f, d, d);
            hits.Add(new Hit(r, action));
        }

        void Metric(Graphics g, RectangleF c, string key, string value, float y, Color? accent)
        {
            Draw.Text(g, key, Fonts.B(11.2f, FontStyle.Regular), p.Dim, c.X, y);
            SizeF m = Draw.Measure(g, value, Fonts.B(11.2f, FontStyle.Bold));
            Draw.Text(g, value, Fonts.B(11.2f, FontStyle.Bold), accent.HasValue ? accent.Value : p.Text,
                c.Right - m.Width, y);
            using (Pen pen = new Pen(Draw.Alpha(p.Outline, 40), 1f))
                g.DrawLine(pen, c.X, y + 24f, c.Right, y + 24f);
        }

        void Wave(Graphics g, RectangleF r, bool active, Color color, int bars)
        {
            float gap = r.Width / bars;
            for (int i = 0; i < bars; i++)
            {
                float n = i / (float)bars;
                float jitter = 0.35f + 0.65f * Math.Abs((float)Math.Sin(wavePhase * 1.7f + n * 9.2f)) *
                    Math.Abs((float)Math.Cos(wavePhase * 0.6f + n * 3.1f));
                if (!active) jitter = 0.16f + 0.12f * Math.Abs((float)Math.Sin(n * 6f));
                float h = 6f + (r.Height - 6f) * jitter;
                RectangleF bar = new RectangleF(r.X + i * gap + gap * 0.28f, r.Y + (r.Height - h) / 2f, gap * 0.44f, h);
                using (GraphicsPath path = Draw.Round(bar, bar.Width / 2f))
                using (SolidBrush b = new SolidBrush(Draw.Alpha(color, active ? 220 : 70)))
                    g.FillPath(b, path);
            }
        }

        void Knob(Graphics g, RectangleF r, float value, float min, float max, string action,
            string center, string label, Color accent)
        {
            float norm = (value - min) / (max - min);
            if (norm < 0f) norm = 0f;
            if (norm > 1f) norm = 1f;
            float inset = 8f;
            using (Pen track = new Pen(p.Sunken, 11f))
            {
                track.StartCap = LineCap.Round; track.EndCap = LineCap.Round;
                g.DrawArc(track, r.X + inset, r.Y + inset, r.Width - inset * 2, r.Height - inset * 2, 135, 270);
            }
            using (Pen fill = new Pen(accent, 11f))
            {
                fill.StartCap = LineCap.Round; fill.EndCap = LineCap.Round;
                g.DrawArc(fill, r.X + inset, r.Y + inset, r.Width - inset * 2, r.Height - inset * 2, 135, 270 * norm);
            }
            double ang = (135 + 270 * norm) * Math.PI / 180.0;
            float cx = r.X + r.Width / 2f;
            float cy = r.Y + r.Height / 2f;
            float rad = r.Width / 2f - inset;
            float kx = cx + (float)Math.Cos(ang) * rad;
            float ky = cy + (float)Math.Sin(ang) * rad;
            using (SolidBrush b = new SolidBrush(accent)) g.FillEllipse(b, kx - 5.5f, ky - 5.5f, 11f, 11f);
            using (SolidBrush b = new SolidBrush(p.Bg)) g.FillEllipse(b, kx - 2.2f, ky - 2.2f, 4.4f, 4.4f);

            SizeF m = Draw.Measure(g, center, Fonts.D(26f, FontStyle.Bold));
            Draw.Text(g, center, Fonts.D(26f, FontStyle.Bold), p.Text, cx - m.Width / 2f, cy - 20f);
            SizeF l = Draw.Measure(g, label.ToUpperInvariant(), Fonts.B(8.6f, FontStyle.Bold));
            Draw.Text(g, label.ToUpperInvariant(), Fonts.B(8.6f, FontStyle.Bold), p.Dim,
                cx - l.Width / 2f, cy + 8f);

            RectangleF hit = new RectangleF(r.X, r.Y, r.Width, r.Height);
            hits.Add(new Hit(hit, action));
        }

        void Bar(Graphics g, RectangleF r, float value, float min, float max, string action, Color accent)
        {
            float norm = (value - min) / (max - min);
            if (norm < 0f) norm = 0f;
            if (norm > 1f) norm = 1f;
            RectangleF track = new RectangleF(r.X, r.Y + r.Height / 2f - 6f, r.Width, 12f);
            using (GraphicsPath path = Draw.Round(track, 6f))
            using (SolidBrush b = new SolidBrush(p.Sunken))
                g.FillPath(b, path);
            if (norm > 0.01f)
            {
                RectangleF fill = new RectangleF(track.X, track.Y, Math.Max(12f, track.Width * norm), 12f);
                using (GraphicsPath path = Draw.Round(fill, 6f))
                using (LinearGradientBrush b = new LinearGradientBrush(fill,
                    Draw.Alpha(accent, 140), accent, 0f))
                    g.FillPath(b, path);
            }
            float kx = track.X + track.Width * norm;
            using (SolidBrush b = new SolidBrush(p.Text)) g.FillEllipse(b, kx - 12f, r.Y + r.Height / 2f - 12f, 24f, 24f);
            using (SolidBrush b = new SolidBrush(p.Bg)) g.FillEllipse(b, kx - 8f, r.Y + r.Height / 2f - 8f, 16f, 16f);
            hits.Add(new Hit(r, action));
        }

        // ------------------------------------------------------------------
        // HOME
        // ------------------------------------------------------------------
        void DrawHome(Graphics g, RectangleF c)
        {
            EndpointInfo ep = Audio.DefaultRender();
            Header(g, c, "REZOFON", "Ses atölyesi", "JAM+ için sistem geneli ekolayzer ve hat yönetimi",
                null, null, null);

            float y = c.Y + 100f;
            float w = c.Width;

            // hero
            RectangleF hero = new RectangleF(c.X, y, w, 186f);
            Draw.Tile(g, p, hero, Draw.Alpha(p.Lime, 26), press.Value, hero.Contains(mouse));
            hits.Add(new Hit(hero, "nav", 3));

            string devName = ep != null ? ep.Name : "Çıkış aygıtı yok";
            bool isJam = devName.IndexOf("Grundig", StringComparison.OrdinalIgnoreCase) >= 0 ||
                         devName.IndexOf("JAM", StringComparison.OrdinalIgnoreCase) >= 0;

            Kicker(g, isJam ? "Ses hattı açık" : "Varsayılan çıkış", hero.X + 24f, hero.Y + 20f);
            using (SolidBrush b = new SolidBrush(isJam ? p.Lime : p.Amber))
                g.FillEllipse(b, hero.X + 24f, hero.Y + 38f, 7f, 7f);
            Draw.Text(g, devName, Fonts.D(27f, FontStyle.Bold), p.Text, hero.X + 36f, hero.Y + 34f);
            Draw.Text(g, ep != null ? "Varsayılan çıkış · " + (ep.Active ? "aktif" : "pasif") : "Bluetooth eşleşmesi bekleniyor",
                Fonts.B(11.5f, FontStyle.Regular), p.Dim, hero.X + 36f, hero.Y + 70f);

            Wave(g, new RectangleF(hero.X + 24f, hero.Y + 96f, w * 0.52f, 44f), true, p.Lime, 46);
            Draw.Text(g, "40 Hz — 16 kHz", Fonts.B(9.4f, FontStyle.Bold), p.Dim, hero.Right - 132f, hero.Y + 24f);
            Draw.Text(g, "3.5 W RMS · mono", Fonts.B(11f, FontStyle.Regular), p.Dim, hero.Right - 192f, hero.Y + 42f);
            Draw.Text(g, eapoApplied ? "EQ: EAPO ile etkin" : "EQ: henüz uygulanmadı",
                Fonts.B(11f, FontStyle.Bold), eapoApplied ? p.Lime : p.Amber, hero.Right - 232f, hero.Y + 66f);

            // volume tile
            RectangleF vol = new RectangleF(hero.Right - 300f, hero.Y + 96f, 262f, 66f);
            Kicker(g, "Sistem sesi", vol.X, vol.Y - 4f);
            float volume = Audio.GetVolume();
            bool muted = Audio.GetMute();
            Draw.Icon(g, "volume", new RectangleF(vol.X, vol.Y + 18f, 18f, 18f), muted ? p.Danger : p.Text);
            Bar(g, new RectangleF(vol.X + 28f, vol.Y + 10f, vol.Width - 82f, 34f), volume, 0f, 1f, "volume", p.Lime);
            Draw.Text(g, muted ? "SESSİZ" : "%" + (int)Math.Round(volume * 100), Fonts.B(10.5f, FontStyle.Bold),
                muted ? p.Danger : p.Text, vol.Right - 46f, vol.Y + 20f);

            y += 198f;

            // row: curve + engine
            float leftW = w * 0.62f - 6f;
            RectangleF curveTile = new RectangleF(c.X, y, leftW, 214f);
            Draw.Tile(g, p, curveTile, Draw.Alpha(p.Lime, 14), press.Value, curveTile.Contains(mouse));
            hits.Add(new Hit(curveTile, "nav", 1));
            Kicker(g, "10 bant ekolayzer", curveTile.X + 22f, curveTile.Y + 18f);
            Preset active = FindPreset(st.ActivePreset);
            Draw.Text(g, active != null ? active.Name : "Özel eğri", Fonts.D(20f, FontStyle.Bold), p.Text,
                curveTile.X + 22f, curveTile.Y + 34f);
            Draw.Text(g, FmtDb(st.Preamp) + " preamp · " + (st.Enabled ? "açık" : "kapalı"),
                Fonts.B(10.8f, FontStyle.Regular), p.Dim, curveTile.X + 22f, curveTile.Y + 62f);
            Draw.Curve(g, new RectangleF(curveTile.X + 22f, curveTile.Y + 88f, curveTile.Width - 44f, 104f),
                st.Gains, st.Sub, st.Bass, p, p.Lime, p.Cyan, false);

            RectangleF engine = new RectangleF(curveTile.Right + 12f, y, c.Right - curveTile.Right - 12f, 214f);
            Draw.Tile(g, p, engine, Draw.Alpha(st.Enabled ? p.Lime : p.Danger, 20), press.Value, engine.Contains(mouse));
            Kicker(g, "Motor", engine.X + 22f, engine.Y + 18f);
            RectangleF power = new RectangleF(engine.X + 22f, engine.Y + 44f, 48f, 48f);
            using (GraphicsPath path = Draw.Round(power, 16f))
            using (SolidBrush b = new SolidBrush(st.Enabled ? p.Lime : p.Sunken))
                g.FillPath(b, path);
            Draw.Icon(g, "power", new RectangleF(power.X + 12f, power.Y + 12f, 24f, 24f), st.Enabled ? p.Ink : p.Dim);
            Draw.Text(g, st.Enabled ? "Açık" : "Kapalı", Fonts.D(20f, FontStyle.Bold), p.Text, engine.X + 22f, engine.Y + 102f);
            Draw.Text(g, eapoApplied ? "Equalizer APO" : "Hazır bekliyor", Fonts.B(10.8f, FontStyle.Regular), p.Dim,
                engine.X + 22f, engine.Y + 128f);
            Toggle(g, new RectangleF(engine.X + 22f, engine.Y + 158f, 50f, 28f), st.Enabled, "toggle_engine");

            y += 226f;

            // row: bass / sub / guard
            float third = (w - 24f) / 3f;
            RectangleF bassTile = new RectangleF(c.X, y, third, 206f);
            Draw.Tile(g, p, bassTile, Draw.Alpha(p.Amber, 16), press.Value, bassTile.Contains(mouse));
            Kicker(g, "Bas derinliği", bassTile.X + 20f, bassTile.Y + 18f);
            hits.Add(new Hit(bassTile, "nav", 2));
            Knob(g, new RectangleF(bassTile.X + (third - 118f) / 2f, bassTile.Y + 40f, 118f, 118f),
                st.Bass, 0f, 1f, "knob_bass", ((int)Math.Round(st.Bass * 100)).ToString(), "punch", p.Amber);

            RectangleF subTile = new RectangleF(bassTile.Right + 12f, y, third, 206f);
            Draw.Tile(g, p, subTile, Draw.Alpha(p.Violet, 16), press.Value, subTile.Contains(mouse));
            Kicker(g, "Sub eşiği", subTile.X + 20f, subTile.Y + 18f);
            hits.Add(new Hit(subTile, "nav", 2));
            Knob(g, new RectangleF(subTile.X + (third - 118f) / 2f, subTile.Y + 40f, 118f, 118f),
                st.Sub, 0f, 9f, "knob_sub", FmtDb(st.Sub).Replace(" dB", ""), "shelf", p.Violet);

            RectangleF guardTile = new RectangleF(subTile.Right + 12f, y, c.Right - subTile.Right - 12f, 206f);
            Draw.Tile(g, p, guardTile, Draw.Alpha(st.Guard ? p.Cyan : p.Text, st.Guard ? 22 : 4),
                press.Value, guardTile.Contains(mouse));
            Kicker(g, "Hat bekçisi", guardTile.X + 20f, guardTile.Y + 18f);
            RectangleF gIcon = new RectangleF(guardTile.X + 20f, guardTile.Y + 42f, 44f, 44f);
            using (GraphicsPath path = Draw.Round(gIcon, 15f))
            using (SolidBrush b = new SolidBrush(st.Guard ? p.Cyan : p.Sunken))
                g.FillPath(b, path);
            Draw.Icon(g, "timer", new RectangleF(gIcon.X + 12f, gIcon.Y + 12f, 20f, 20f), st.Guard ? p.Ink : p.Dim);
            Draw.Text(g, st.Guard ? "Sayaç sıfırlanıyor" : "15 dk uyku", Fonts.D(17f, FontStyle.Bold), p.Text,
                guardTile.X + 76f, guardTile.Y + 42f);
            Draw.Text(g, st.Guard ? "Sessiz hat açık" : "Sessizlikte kapanmayı engelle",
                Fonts.B(10.4f, FontStyle.Regular), p.Dim, guardTile.X + 76f, guardTile.Y + 66f);
            Toggle(g, new RectangleF(guardTile.X + 20f, guardTile.Y + 104f, 50f, 28f), st.Guard, "toggle_guard");
            Draw.Text(g, "Detaylar için Atölye", Fonts.B(9.6f, FontStyle.Bold), p.Dim,
                guardTile.X + 82f, guardTile.Y + 111f);

            y += 218f;

            // presets
            Kicker(g, "Hazır eğriler", c.X + 4f, y);
            y += 18f;
            List<Preset> presets = Catalog.Presets();
            float px = c.X;
            for (int i = 0; i < presets.Count; i++)
            {
                SizeF m = Draw.Measure(g, presets[i].Name, Fonts.B(10.8f, FontStyle.Bold));
                float cw = m.Width + 34f;
                if (px + cw > c.Right)
                {
                    px = c.X;
                    y += 46f;
                }
                Chip(g, new RectangleF(px, y, cw, 36f), presets[i].Name,
                    st.ActivePreset == presets[i].Id, "preset", i);
                px += cw + 8f;
            }
            y += 54f;

            // footer note
            RectangleF note = new RectangleF(c.X, y, w, 74f);
            using (GraphicsPath path = Draw.Round(note, 18f))
            using (SolidBrush b = new SolidBrush(Draw.Alpha(p.Cyan, 14)))
                g.FillPath(b, path);
            using (Pen pen = new Pen(Draw.Alpha(p.Cyan, 46), 1f))
            using (GraphicsPath path = Draw.Round(note, 18f))
                g.DrawPath(pen, path);
            Draw.Text(g, "Kırpma koruması etkin. Bas yükseldikçe ön kazanç otomatik düşürülür; " +
                "EQ değişikliklerini duymak için Atölye ekranından uygulayın.",
                Fonts.B(10.8f, FontStyle.Regular), Draw.Alpha(p.Text, 210),
                new RectangleF(note.X + 18f, note.Y + 14f, note.Width - 36f, note.Height - 24f), true);

            scrolls["home"] = ClampScroll(c, y + 100f);
        }

        // ------------------------------------------------------------------
        // EQ
        // ------------------------------------------------------------------
        void DrawEq(Graphics g, RectangleF c)
        {
            Header(g, c, "EKOLAYZER", "10 bant", "±12 dB · logaritmik eğri · Equalizer APO köprüsü",
                "apply_apo", "eq", eapoApplied ? "Yeniden uygula" : "Uygula");

            float y = c.Y + 104f;
            RectangleF curveTile = new RectangleF(c.X, y, c.Width, 276f);
            Draw.Tile(g, p, curveTile, Draw.Alpha(p.Lime, 12), press.Value, false);
            Kicker(g, "Yanıt eğrisi", curveTile.X + 22f, curveTile.Y + 18f);
            Draw.Text(g, FmtDb(st.Preamp) + " preamp · headroom", Fonts.B(10.6f, FontStyle.Bold), p.Dim,
                curveTile.Right - 190f, curveTile.Y + 18f);
            Draw.Curve(g, new RectangleF(curveTile.X + 22f, curveTile.Y + 46f, curveTile.Width - 44f, 176f),
                st.Gains, st.Sub, st.Bass, p, p.Lime, p.Cyan, true);
            for (int i = 0; i < 10; i++)
            {
                float x = curveTile.X + 22f + (curveTile.Width - 44f) * (i + 0.5f) / 10f;
                SizeF m = Draw.Measure(g, Catalog.Labels[i], Fonts.B(9f, FontStyle.Bold));
                Draw.Text(g, Catalog.Labels[i], Fonts.B(9f, FontStyle.Bold), p.Dim,
                    x - m.Width / 2f, curveTile.Y + 232f);
            }

            y += 288f;

            RectangleF faders = new RectangleF(c.X, y, c.Width, 262f);
            Draw.Tile(g, p, faders, null, press.Value, false);
            Kicker(g, "Bant kazançları", faders.X + 22f, faders.Y + 18f);
            Draw.Text(g, "sürükle · ±12 dB", Fonts.B(9.4f, FontStyle.Bold), p.Dim, faders.Right - 118f, faders.Y + 18f);
            float slot = (faders.Width - 44f) / 10f;
            for (int i = 0; i < 10; i++)
            {
                float cx = faders.X + 22f + slot * (i + 0.5f);
                float top = faders.Y + 56f;
                float h = 148f;
                Color accent = i < 3 ? p.Amber : (i < 7 ? p.Lime : p.Cyan);
                float value = st.Gains[i];
                float norm = (value + 12f) / 24f;
                float knobY = top + (1f - norm) * h;

                using (Pen pen = new Pen(p.Sunken, 6f))
                {
                    pen.StartCap = LineCap.Round; pen.EndCap = LineCap.Round;
                    g.DrawLine(pen, cx, top, cx, top + h);
                }
                using (Pen pen = new Pen(Draw.Alpha(p.Dim, 60), 1f))
                    g.DrawLine(pen, cx - 8f, top + h / 2f, cx + 8f, top + h / 2f);
                using (Pen pen = new Pen(accent, 6f))
                {
                    pen.StartCap = LineCap.Round; pen.EndCap = LineCap.Round;
                    g.DrawLine(pen, cx, top + h / 2f, cx, knobY);
                }
                using (SolidBrush b = new SolidBrush(accent)) g.FillEllipse(b, cx - 8f, knobY - 8f, 16f, 16f);
                using (SolidBrush b = new SolidBrush(p.Bg)) g.FillEllipse(b, cx - 3f, knobY - 3f, 6f, 6f);

                string txt = (value > 0.05f ? "+" : "") + Math.Round(value).ToString("0");
                SizeF m = Draw.Measure(g, txt, Fonts.B(9.4f, FontStyle.Bold));
                Draw.Text(g, txt, Fonts.B(9.4f, FontStyle.Bold), value > 0.05f ? accent : p.Dim,
                    cx - m.Width / 2f, top - 18f);
                SizeF lm = Draw.Measure(g, Catalog.Labels[i], Fonts.B(9.2f, FontStyle.Bold));
                Draw.Text(g, Catalog.Labels[i], Fonts.B(9.2f, FontStyle.Bold), p.Dim,
                    cx - lm.Width / 2f, top + h + 12f);

                RectangleF hit = new RectangleF(cx - slot / 2f, top - 20f, slot, h + 46f);
                hits.Add(new Hit(hit, "fader", i));
            }

            y += 274f;

            float half = (c.Width - 12f) / 2f;
            RectangleF pre = new RectangleF(c.X, y, half, 168f);
            Draw.Tile(g, p, pre, null, press.Value, false);
            Kicker(g, "Ön kazanç", pre.X + 22f, pre.Y + 18f);
            Draw.Text(g, FmtDb(st.Preamp), Fonts.D(20f, FontStyle.Bold), p.Text, pre.Right - 96f, pre.Y + 14f);
            Draw.Text(g, "Önerilen: " + FmtDb(Catalog.SuggestedPreamp(st.Gains, st.Sub, st.Bass)),
                Fonts.B(10.2f, FontStyle.Regular), p.Dim, pre.X + 22f, pre.Y + 50f);
            Bar(g, new RectangleF(pre.X + 22f, pre.Y + 78f, pre.Width - 44f, 36f), st.Preamp, -12f, 0f, "preamp", p.Cyan);
            RectangleF flat = new RectangleF(pre.X + 22f, pre.Y + 122f, 96f, 32f);
            Chip(g, flat, "Düzleştir", false, "flatten", 0);

            RectangleF actions = new RectangleF(pre.Right + 12f, y, c.Right - pre.Right - 12f, 168f);
            Draw.Tile(g, p, actions, Draw.Alpha(p.Violet, 12), press.Value, false);
            Kicker(g, "Equalizer APO", actions.X + 22f, actions.Y + 18f);
            Draw.Text(g, apo.Status(), Fonts.D(17f, FontStyle.Bold), p.Text, actions.X + 22f, actions.Y + 40f);
            Draw.Text(g, "Sistem geneli EQ, APO motoruyla uygulanır.\nAyarlar config.txt içine ayrı blok olarak yazılır.",
                Fonts.B(10.4f, FontStyle.Regular), p.Dim,
                new RectangleF(actions.X + 22f, actions.Y + 68f, actions.Width - 44f, 44f), true);
            Chip(g, new RectangleF(actions.X + 22f, actions.Y + 122f, 96f, 32f), "Uygula", true, "apply_apo", 0);
            Chip(g, new RectangleF(actions.X + 126f, actions.Y + 122f, 84f, 32f), "Kaldır", false, "remove_apo", 0);

            y += 180f;
            Kicker(g, "Hazır eğriler", c.X + 4f, y);
            y += 18f;
            List<Preset> presets = Catalog.Presets();
            float px = c.X;
            for (int i = 0; i < presets.Count; i++)
            {
                SizeF m = Draw.Measure(g, presets[i].Name, Fonts.B(10.8f, FontStyle.Bold));
                float cw = m.Width + 34f;
                if (px + cw > c.Right) { px = c.X; y += 46f; }
                Chip(g, new RectangleF(px, y, cw, 36f), presets[i].Name,
                    st.ActivePreset == presets[i].Id, "preset", i);
                px += cw + 8f;
            }

            scrolls["eq"] = ClampScroll(c, y + 120f);
        }

        // ------------------------------------------------------------------
        // BASS
        // ------------------------------------------------------------------
        void DrawBass(Graphics g, RectangleF c)
        {
            Header(g, c, "BAS VE GÖVDE", "Derinlik", "Pasif radyatör · punch · düşük frekans rafı", null, null, null);

            float y = c.Y + 104f;
            RectangleF hero = new RectangleF(c.X, y, c.Width, 252f);
            Draw.Tile(g, p, hero, Draw.Alpha(p.Amber, 26), press.Value, false);

            float cx = hero.X + 150f;
            float cy = hero.Y + hero.Height / 2f;
            float energy = 0.25f + st.Bass * 0.55f + st.Sub / 9f * 0.2f;
            for (int ring = 0; ring < 4; ring++)
            {
                float rr = 52f + ring * 20f + (float)Math.Sin(wavePhase + ring * 0.8f) * 6f * energy;
                using (Pen pen = new Pen(Draw.Alpha(p.Amber, (int)(120 - ring * 24)), 1.4f))
                    g.DrawEllipse(pen, cx - rr, cy - rr, rr * 2f, rr * 2f);
            }
            using (GraphicsPath path = new GraphicsPath())
            {
                path.AddEllipse(cx - 54f, cy - 54f, 108f, 108f);
                using (PathGradientBrush pb = new PathGradientBrush(path))
                {
                    pb.CenterColor = Draw.Alpha(p.Amber, 150);
                    pb.SurroundColors = new Color[] { Draw.Alpha(p.Amber, 10) };
                    g.FillPath(pb, path);
                }
            }
            using (SolidBrush b = new SolidBrush(Draw.Alpha(p.Bg, 140))) g.FillEllipse(b, cx - 24f, cy - 24f, 48f, 48f);
            using (SolidBrush b = new SolidBrush(p.Amber)) g.FillEllipse(b, cx - 4f, cy - 4f, 8f, 8f);

            Kicker(g, "Radyatör", hero.X + 24f, hero.Y + 22f);
            Draw.Text(g, st.BassEnabled ? "Radyatör aktif" : "Radyatör beklemede", Fonts.D(24f, FontStyle.Bold), p.Text,
                hero.X + 24f, hero.Y + 40f);
            Draw.Text(g, "JAM+ 100 Hz altını sürücüyle değil, pasif radyatörle taşır.\nPunch bu yüzden 100–250 Hz gövdesinde duyulur.",
                Fonts.B(11f, FontStyle.Regular), p.Dim,
                new RectangleF(hero.X + 24f, hero.Y + 76f, 330f, 50f), true);

            float kx = hero.Right - 520f;
            Knob(g, new RectangleF(kx, hero.Y + 48f, 132f, 132f), st.Bass, 0f, 1f, "knob_bass",
                ((int)Math.Round(st.Bass * 100)).ToString(), "punch", p.Amber);
            Knob(g, new RectangleF(kx + 176f, hero.Y + 48f, 132f, 132f), st.Sub, 0f, 9f, "knob_sub",
                FmtDb(st.Sub).Replace(" dB", ""), "shelf", p.Violet);
            Draw.Text(g, "Punch", Fonts.B(10.4f, FontStyle.Bold), p.Dim, kx + 52f, hero.Y + 190f);
            Draw.Text(g, "Sub eşiği", Fonts.B(10.4f, FontStyle.Bold), p.Dim, kx + 222f, hero.Y + 190f);
            Draw.Text(g, "Önerilen üst sınır %78", Fonts.B(9.6f, FontStyle.Bold), Draw.Alpha(p.Amber, 220),
                kx + 8f, hero.Y + 212f);

            y += 264f;

            float half = (c.Width - 12f) / 2f;
            RectangleF depth = new RectangleF(c.X, y, half, 214f);
            Draw.Tile(g, p, depth, null, press.Value, false);
            Kicker(g, "Bas derinliği", depth.X + 22f, depth.Y + 18f);
            Draw.Text(g, ((int)Math.Round(st.Bass * 100)).ToString(), Fonts.D(30f, FontStyle.Bold), p.Text,
                depth.X + 22f, depth.Y + 36f);
            Draw.Text(g, "Pasif radyatör hareketini artırır. 3.5 W sürücüde\n%78 üstü distorsiyona girer.",
                Fonts.B(10.6f, FontStyle.Regular), p.Dim,
                new RectangleF(depth.X + 100f, depth.Y + 40f, depth.Width - 122f, 46f), true);
            Bar(g, new RectangleF(depth.X + 22f, depth.Y + 98f, depth.Width - 44f, 38f), st.Bass, 0f, 1f, "bar_bass", p.Amber);
            Toggle(g, new RectangleF(depth.X + 22f, depth.Y + 156f, 50f, 28f), st.BassEnabled, "toggle_bass");
            Draw.Text(g, "Bas katmanı", Fonts.B(10.6f, FontStyle.Bold), p.Dim, depth.X + 84f, depth.Y + 163f);

            RectangleF sub = new RectangleF(depth.Right + 12f, y, c.Right - depth.Right - 12f, 214f);
            Draw.Tile(g, p, sub, Draw.Alpha(p.Violet, 12), press.Value, false);
            Kicker(g, "Sub eşiği", sub.X + 22f, sub.Y + 18f);
            Draw.Text(g, FmtDb(st.Sub), Fonts.D(30f, FontStyle.Bold), p.Text, sub.X + 22f, sub.Y + 36f);
            Draw.Text(g, "Düşük frekans rafı. 62 Hz yüksek geçiren filtre\nher zaman açık kalır.",
                Fonts.B(10.6f, FontStyle.Regular), p.Dim,
                new RectangleF(sub.X + 136f, sub.Y + 40f, sub.Width - 158f, 46f), true);
            Bar(g, new RectangleF(sub.X + 22f, sub.Y + 98f, sub.Width - 44f, 38f), st.Sub, 0f, 9f, "bar_sub", p.Violet);
            Draw.Text(g, "0 dB · temiz", Fonts.B(9.4f, FontStyle.Bold), p.Dim, sub.X + 22f, sub.Y + 146f);
            Draw.Text(g, "+9 dB · maksimum", Fonts.B(9.4f, FontStyle.Bold), p.Dim, sub.Right - 132f, sub.Y + 146f);

            y += 226f;

            RectangleF loud = new RectangleF(c.X, y, c.Width, 132f);
            Draw.Tile(g, p, loud, Draw.Alpha(p.Cyan, 12), press.Value, false);
            Kicker(g, "Gece yüksekliği", loud.X + 22f, loud.Y + 18f);
            Draw.Text(g, "Algısal eşitleme", Fonts.D(20f, FontStyle.Bold), p.Text, loud.X + 22f, loud.Y + 34f);
            Draw.Text(g, "Düşük seste bas ve tiz algısını dengeler.", Fonts.B(10.6f, FontStyle.Regular), p.Dim,
                loud.X + 22f, loud.Y + 64f);
            Bar(g, new RectangleF(loud.X + 340f, loud.Y + 46f, loud.Width - 450f, 38f), st.Loudness, 0f, 1f, "bar_loud", p.Cyan);
            Draw.Text(g, ((int)Math.Round(st.Loudness * 100)).ToString() + "%", Fonts.D(22f, FontStyle.Bold), p.Cyan,
                loud.Right - 90f, loud.Y + 48f);

            scrolls["bass"] = ClampScroll(c, y + 220f);
        }

        // ------------------------------------------------------------------
        // DEVICE
        // ------------------------------------------------------------------
        void DrawDevice(Graphics g, RectangleF c)
        {
            Header(g, c, "CİHAZ", "Bağlantı", "Çıkış uç noktaları · sürücü · kodek", "refresh_dev", "refresh", "Yenile");

            float y = c.Y + 104f;
            List<EndpointInfo> eps = Audio.RenderEndpoints();
            EndpointInfo def = Audio.DefaultRender();
            string devName = def != null ? def.Name : "Çıkış yok";
            bool isJam = devName.IndexOf("Grundig", StringComparison.OrdinalIgnoreCase) >= 0;

            RectangleF hero = new RectangleF(c.X, y, c.Width, 126f);
            Draw.Tile(g, p, hero, Draw.Alpha(p.Lime, 22), press.Value, false);
            RectangleF icon = new RectangleF(hero.X + 24f, hero.Y + 30f, 62f, 62f);
            using (GraphicsPath path = Draw.Round(icon, 20f))
            using (SolidBrush b = new SolidBrush(isJam ? p.Lime : p.SurfaceAlt))
                g.FillPath(b, path);
            Draw.Icon(g, "device", new RectangleF(icon.X + 18f, icon.Y + 18f, 26f, 26f), isJam ? p.Ink : p.Dim);
            Draw.Text(g, devName, Fonts.D(24f, FontStyle.Bold), p.Text, hero.X + 102f, hero.Y + 30f);
            Draw.Text(g, (def != null ? (def.Active ? "Aktif" : "Pasif") : "—") + " · varsayılan çıkış · " +
                (isJam ? "A2DP akışı hazır" : "JAM+ seçili değil"),
                Fonts.B(11f, FontStyle.Regular), p.Dim, hero.X + 102f, hero.Y + 64f);
            Draw.Text(g, "Ses uç noktaları Windows Core Audio üzerinden okunur.",
                Fonts.B(10.2f, FontStyle.Bold), Draw.Alpha(p.Dim, 200), hero.X + 102f, hero.Y + 86f);

            y += 138f;
            float half = (c.Width - 12f) / 2f;

            RectangleF list = new RectangleF(c.X, y, half, 236f);
            Draw.Tile(g, p, list, null, press.Value, false);
            Kicker(g, "Çıkış uç noktaları", list.X + 22f, list.Y + 18f);
            float ly = list.Y + 44f;
            int shown = 0;
            for (int i = 0; i < eps.Count && shown < 5; i++)
            {
                EndpointInfo ep = eps[i];
                if (ep.Name.Length == 0) continue;
                using (SolidBrush b = new SolidBrush(ep.IsDefault ? p.Lime : p.Dim))
                    g.FillEllipse(b, list.X + 22f, ly + 6f, 6f, 6f);
                Draw.Text(g, Shorten(ep.Name, 40), Fonts.B(11f, ep.IsDefault ? FontStyle.Bold : FontStyle.Regular),
                    ep.IsDefault ? p.Text : p.Dim, list.X + 36f, ly);
                Draw.Text(g, ep.IsDefault ? "varsayılan" : (ep.Active ? "hazır" : "pasif"),
                    Fonts.B(9.4f, FontStyle.Bold), ep.IsDefault ? p.Lime : Draw.Alpha(p.Dim, 180), list.Right - 76f, ly + 1f);
                ly += 30f;
                shown++;
            }
            if (shown == 0)
                Draw.Text(g, "Uç nokta bulunamadı.", Fonts.B(11f, FontStyle.Regular), p.Dim, list.X + 22f, ly);

            RectangleF drv = new RectangleF(list.Right + 12f, y, c.Right - list.Right - 12f, 236f);
            Draw.Tile(g, p, drv, null, press.Value, false);
            Kicker(g, "Sürücü ve arabirim", drv.X + 22f, drv.Y + 18f);
            List<string[]> info = DriverInfo();
            float dy = drv.Y + 46f;
            for (int i = 0; i < info.Count && i < 5; i++)
            {
                Draw.Text(g, info[i][0], Fonts.B(10.6f, FontStyle.Regular), p.Dim, drv.X + 22f, dy);
                Draw.Text(g, Shorten(info[i][1], 30), Fonts.B(10.6f, FontStyle.Bold), p.Text, drv.X + 150f, dy);
                dy += 28f;
            }
            Chip(g, new RectangleF(drv.X + 22f, drv.Bottom - 52f, 128f, 32f), "Aygıt yöneticisi", false, "open_devmgr", 0);

            y += 248f;

            RectangleF apoBox = new RectangleF(c.X, y, c.Width, 168f);
            Draw.Tile(g, p, apoBox, Draw.Alpha(p.Violet, 14), press.Value, false);
            Kicker(g, "Equalizer APO", apoBox.X + 22f, apoBox.Y + 18f);
            Draw.Text(g, apo.Status(), Fonts.D(19f, FontStyle.Bold), p.Text, apoBox.X + 22f, apoBox.Y + 36f);
            Draw.Text(g, "Yol: " + apo.ConfigDir, Fonts.B(10.4f, FontStyle.Regular), p.Dim, apoBox.X + 22f, apoBox.Y + 68f);
            Draw.Text(g, "APO yalnızca DeviceSelector'da işaretlenen aygıtlara uygulanır.\nJAM+ görünmüyorsa önce onu ekleyin.",
                Fonts.B(10.4f, FontStyle.Regular), p.Dim,
                new RectangleF(apoBox.X + 22f, apoBox.Y + 90f, 420f, 44f), true);
            Chip(g, new RectangleF(apoBox.Right - 260f, apoBox.Y + 56f, 112f, 34f), "DeviceSelector", false, "open_devsel", 0);
            Chip(g, new RectangleF(apoBox.Right - 140f, apoBox.Y + 56f, 118f, 34f), "Editor", false, "open_editor", 0);
            Chip(g, new RectangleF(apoBox.Right - 260f, apoBox.Y + 100f, 238f, 34f),
                eapoApplied ? "Bloğu kaldır" : "Bloğu bağla", !eapoApplied, eapoApplied ? "remove_apo" : "apply_apo", 0);

            y += 180f;

            RectangleF note = new RectangleF(c.X, y, c.Width, 92f);
            using (GraphicsPath path = Draw.Round(note, 18f))
            using (SolidBrush b = new SolidBrush(Draw.Alpha(p.Amber, 14)))
                g.FillPath(b, path);
            using (GraphicsPath path = Draw.Round(note, 18f))
            using (Pen pen = new Pen(Draw.Alpha(p.Amber, 46), 1f))
                g.DrawPath(pen, path);
            Draw.Text(g, "Grundig JAM+ için herkese açık bir ürün yazılımı (OTA) kanalı yok; üretici bu modelin " +
                "mobil uygulama desteğini de kaldırdı. Rezofon sahte bir \"güncelleme\" vaadi vermez — " +
                "sürücü ve arabirim bilgisini gösterir, ses işleme zincirini telefon/PC tarafında kurar.",
                Fonts.B(10.8f, FontStyle.Regular), Draw.Alpha(p.Text, 215),
                new RectangleF(note.X + 18f, note.Y + 14f, note.Width - 36f, note.Height - 24f), true);

            scrolls["device"] = ClampScroll(c, y + 200f);
        }

        // ------------------------------------------------------------------
        // WORKSHOP
        // ------------------------------------------------------------------
        void DrawWorkshop(Graphics g, RectangleF c)
        {
            Header(g, c, "ATÖLYE", "Derin ayarlar", "Motor durumu · paylaşım · bekçi", null, null, null);

            float y = c.Y + 104f;
            float half = (c.Width - 12f) / 2f;

            RectangleF motor = new RectangleF(c.X, y, half, 236f);
            Draw.Tile(g, p, motor, null, press.Value, false);
            Kicker(g, "Ses işleme zinciri", motor.X + 22f, motor.Y + 18f);
            Draw.Text(g, eapoApplied ? "Etkin" : "Beklemede", Fonts.D(24f, FontStyle.Bold),
                eapoApplied ? p.Lime : p.Amber, motor.X + 22f, motor.Y + 38f);
            string[] rows = new string[] {
                "Ön kazanç|" + FmtDb(st.Preamp),
                "Bas katmanı|" + (st.BassEnabled ? "açık" : "kapalı"),
                "Punch|%" + ((int)Math.Round(st.Bass * 100)),
                "Sub rafı|" + FmtDb(st.Sub),
                "Yükseklik|%" + ((int)Math.Round(st.Loudness * 100)),
                "APO bloğu|" + (eapoApplied ? "bağlı" : "yok")
            };
            float ry = motor.Y + 76f;
            for (int i = 0; i < rows.Length; i++)
            {
                string[] parts = rows[i].Split('|');
                Metric(g, new RectangleF(motor.X + 22f, 0, motor.Width - 44f, 0), parts[0], parts[1], ry,
                    parts[1] == "bağlı" || parts[1] == "açık" ? p.Lime : (Color?)null);
                ry += 26f;
            }

            RectangleF guard = new RectangleF(motor.Right + 12f, y, c.Right - motor.Right - 12f, 236f);
            Draw.Tile(g, p, guard, Draw.Alpha(p.Cyan, 14), press.Value, false);
            Kicker(g, "Hat bekçisi", guard.X + 22f, guard.Y + 18f);
            Draw.Text(g, st.Guard ? "Çalışıyor" : "Kapalı", Fonts.D(24f, FontStyle.Bold), p.Text,
                guard.X + 22f, guard.Y + 38f);
            Draw.Text(g, "Sessiz bir ses akışı açık tutularak hoparlörün uyku sayacı sıfırlanır. " +
                "Varsayılan çıkış JAM+ olduğunda etkilidir.",
                Fonts.B(10.8f, FontStyle.Regular), p.Dim,
                new RectangleF(guard.X + 22f, guard.Y + 76f, guard.Width - 44f, 56f), true);
            Toggle(g, new RectangleF(guard.X + 22f, guard.Y + 146f, 50f, 28f), st.Guard, "toggle_guard");
            Draw.Text(g, st.Guard ? "Bekçiyi durdur" : "Bekçiyi başlat", Fonts.B(11f, FontStyle.Bold), p.Text,
                guard.X + 84f, guard.Y + 152f);
            Chip(g, new RectangleF(guard.X + 22f, guard.Y + 186f, 152f, 32f), "Windows ses karıştırıcı", false, "open_mixer", 0);

            y += 248f;

            RectangleF share = new RectangleF(c.X, y, c.Width, 176f);
            Draw.Tile(g, p, share, Draw.Alpha(p.Lime, 12), press.Value, false);
            Kicker(g, "Profil paylaşımı", share.X + 22f, share.Y + 18f);
            Draw.Text(g, "JSON biçimi", Fonts.D(20f, FontStyle.Bold), p.Text, share.X + 22f, share.Y + 36f);
            Draw.Text(g, "Android sürümüyle aynı biçim (rezofon.profile/1). Profili panoya alıp " +
                "telefonda Ayarlar → JSON içe al ekranına yapıştırabilirsiniz.",
                Fonts.B(10.8f, FontStyle.Regular), p.Dim,
                new RectangleF(share.X + 22f, share.Y + 68f, share.Width - 44f, 46f), true);
            Chip(g, new RectangleF(share.X + 22f, share.Y + 124f, 118f, 34f), "Panoya kopyala", true, "copy_json", 0);
            Chip(g, new RectangleF(share.X + 148f, share.Y + 124f, 104f, 34f), "Dosyaya yaz", false, "save_json", 0);

            y += 188f;

            RectangleF about = new RectangleF(c.X, y, c.Width, 150f);
            Draw.Tile(g, p, about, null, press.Value, false);
            Kicker(g, "Hakkında", about.X + 22f, about.Y + 18f);
            Draw.Text(g, "Rezofon 1.0.0", Fonts.D(20f, FontStyle.Bold), p.Text, about.X + 22f, about.Y + 36f);
            Draw.Text(g, "Grundig JAM+ gibi kendi DSP'si olmayan Bluetooth hoparlörler için PC tarafında " +
                "çalışan ses atölyesi. Veri toplamaz, ağ bağlantısı kurmaz. MIT lisansı.",
                Fonts.B(10.8f, FontStyle.Regular), p.Dim,
                new RectangleF(about.X + 22f, about.Y + 68f, about.Width - 44f, 46f), true);
            Draw.Text(g, "MIT · studio.rezofon.app · github.com/tutualican/rezofon",
                Fonts.B(9.8f, FontStyle.Bold), Draw.Alpha(p.Dim, 200), about.X + 22f, about.Y + 116f);

            scrolls["workshop"] = ClampScroll(c, y + 220f);
        }

        // ------------------------------------------------------------------
        float ClampScroll(RectangleF c, float contentBottom)
        {
            float max = Math.Max(0f, contentBottom - c.Bottom + 20f);
            float cur = 0f;
            if (scrolls.ContainsKey(screen)) cur = scrolls[screen];
            if (cur > max) cur = max;
            if (cur < 0f) cur = 0f;
            return cur;
        }

        static string Shorten(string s, int len)
        {
            if (s == null) return "";
            if (s.Length <= len) return s;
            return s.Substring(0, len - 1) + "…";
        }

        Preset FindPreset(string id)
        {
            List<Preset> all = Catalog.Presets();
            for (int i = 0; i < all.Count; i++) if (all[i].Id == id) return all[i];
            return null;
        }

        static string FmtDb(float v)
        {
            float r = (float)Math.Round(v * 10f) / 10f;
            if (Math.Abs(r) < 0.05f) return "0.0 dB";
            if (r > 0f) return "+" + r.ToString("0.0") + " dB";
            return r.ToString("0.0") + " dB";
        }

        List<string[]> DriverInfo()
        {
            List<string[]> rows = new List<string[]>();
            rows.Add(new string[] { "Platform", "Windows " + Environment.OSVersion.Version.Major });
            try
            {
                using (System.Management.ManagementObjectSearcher searcher =
                    new System.Management.ManagementObjectSearcher("SELECT Name, Status FROM Win32_SoundDevice"))
                {
                    foreach (System.Management.ManagementObject mo in searcher.Get())
                    {
                        string name = Convert.ToString(mo["Name"]);
                        string status = Convert.ToString(mo["Status"]);
                        rows.Add(new string[] { "Ses aygıtı", name });
                        rows.Add(new string[] { "Durum", status });
                        if (rows.Count >= 4) break;
                    }
                }
            }
            catch
            {
                rows.Add(new string[] { "Ses aygıtı", "WMI okunamadı" });
            }
            rows.Add(new string[] { "Kanal", "Stereo · 48 kHz" });
            return rows;
        }

        // ------------------------------------------------------------------
        // Interaction
        // ------------------------------------------------------------------
        void OnWheel(object sender, MouseEventArgs e)
        {
            float cur = scrolls.ContainsKey(screen) ? scrolls[screen] : 0f;
            cur -= e.Delta * 0.55f;
            if (cur < 0f) cur = 0f;
            scrolls[screen] = cur;
            Invalidate();
        }

        protected override void OnMouseDown(MouseEventArgs e)
        {
            base.OnMouseDown(e);
            Hit h = TopAt(e.Location);
            dragAction = null;
            pressedAction = null;
            if (h == null) return;

            press.Value = 0.962f;
            press.Target = 1f;
            pressedAction = h.Action;

            switch (h.Action)
            {
                case "nav":
                    screen = new string[] { "home", "eq", "bass", "device", "workshop" }[h.Index];
                    Invalidate();
                    return;
                case "fader":
                    dragAction = "fader"; dragIndex = h.Index;
                    dragStartY = e.Y;
                    dragStartValue = st.Gains[h.Index];
                    ApplyFader(e.Y);
                    return;
                case "knob_bass":
                    dragAction = "knob_bass"; dragStartY = e.Y; dragStartValue = st.Bass;
                    return;
                case "knob_sub":
                    dragAction = "knob_sub"; dragStartY = e.Y; dragStartValue = st.Sub;
                    return;
                case "bar_bass":
                    st.Bass = ValueFromBar(e.X, h.Rect, 0f, 1f);
                    Commit();
                    dragAction = "bar_bass";
                    return;
                case "bar_sub":
                    st.Sub = ValueFromBar(e.X, h.Rect, 0f, 9f);
                    Commit();
                    dragAction = "bar_sub";
                    return;
                case "bar_loud":
                    st.Loudness = ValueFromBar(e.X, h.Rect, 0f, 1f);
                    Commit();
                    dragAction = "bar_loud";
                    return;
                case "preamp":
                    st.Preamp = ValueFromBar(e.X, h.Rect, -12f, 0f);
                    Commit();
                    dragAction = "preamp";
                    return;
                case "volume":
                    Audio.SetVolume(ValueFromBar(e.X, h.Rect, 0f, 1f));
                    Invalidate();
                    dragAction = "volume";
                    return;
                case "preset":
                    st.ApplyPreset(Catalog.Presets()[h.Index]);
                    Commit();
                    ShowToast("Eğri uygulandı: " + st.ActivePreset);
                    return;
                case "toggle_engine":
                    st.Enabled = !st.Enabled; Commit();
                    return;
                case "toggle_bass":
                    st.BassEnabled = !st.BassEnabled; Commit();
                    return;
                case "toggle_guard":
                    st.Guard = !st.Guard;
                    if (st.Guard) StartGuard(); else StopGuard();
                    st.Save();
                    ShowToast(st.Guard ? "Hat bekçisi açıldı" : "Hat bekçisi durduruldu");
                    return;
                case "apply_apo":
                    ApplyApo();
                    return;
                case "remove_apo":
                    string err = apo.Remove();
                    eapoApplied = apo.HasBlock();
                    ShowToast(err ?? "Rezofon bloğu kaldırıldı");
                    Invalidate();
                    return;
                case "flatten":
                    for (int i = 0; i < 10; i++) st.Gains[i] = 0f;
                    st.Preamp = 0f;
                    st.ActivePreset = "";
                    Commit();
                    ShowToast("Eğri düzleştirildi");
                    return;
                case "refresh_dev":
                    Invalidate();
                    ShowToast("Uç noktalar yenilendi");
                    return;
                case "open_devmgr":
                    try { Process.Start("devmgmt.msc"); } catch { }
                    return;
                case "open_devsel":
                    LaunchApo("DeviceSelector.exe");
                    return;
                case "open_editor":
                    LaunchApo("Editor.exe");
                    return;
                case "open_mixer":
                    try { Process.Start("sndvol.exe"); } catch { }
                    return;
                case "copy_json":
                    try { Clipboard.SetText(st.ToJson()); ShowToast("Profil panoya kopyalandı"); }
                    catch { ShowToast("Panoya kopyalanamadı"); }
                    return;
                case "save_json":
                    using (SaveFileDialog dlg = new SaveFileDialog())
                    {
                        dlg.Filter = "Rezofon profili (*.json)|*.json";
                        dlg.FileName = "rezofon-profil.json";
                        if (dlg.ShowDialog(this) == DialogResult.OK)
                        {
                            try
                            {
                                File.WriteAllText(dlg.FileName, st.ToJson(), Encoding.UTF8);
                                ShowToast("Profil kaydedildi");
                            }
                            catch (Exception ex) { ShowToast(ex.Message); }
                        }
                    }
                    return;
            }
        }

        protected override void OnMouseMove(MouseEventArgs e)
        {
            base.OnMouseMove(e);
            mouse = e.Location;
            if (dragAction == null)
            {
                Cursor = TopAt(e.Location) != null ? Cursors.Hand : Cursors.Default;
                Invalidate();
                return;
            }

            float delta = (dragStartY - e.Y) / 220f;
            if (dragAction == "fader") ApplyFader(e.Y);
            else if (dragAction == "knob_bass") { st.Bass = Clamp01(dragStartValue + delta * 1.6f); Commit(); }
            else if (dragAction == "knob_sub") { st.Sub = Clamp(dragStartValue + delta * 9f * 1.6f, 0f, 9f); Commit(); }
            else if (dragAction == "bar_bass") { st.Bass = BarAt(e, 0f, 1f); Commit(); }
            else if (dragAction == "bar_sub") { st.Sub = BarAt(e, 0f, 9f); Commit(); }
            else if (dragAction == "bar_loud") { st.Loudness = BarAt(e, 0f, 1f); Commit(); }
            else if (dragAction == "preamp") { st.Preamp = BarAt(e, -12f, 0f); Commit(); }
            else if (dragAction == "volume") { Audio.SetVolume(BarAt(e, 0f, 1f)); Invalidate(); }
        }

        protected override void OnMouseUp(MouseEventArgs e)
        {
            base.OnMouseUp(e);
            if (dragAction != null) { st.Save(); ApplyApoSilently(); }
            dragAction = null;
            pressedAction = null;
            press.Target = 1f;
            Invalidate();
        }

        float BarAt(MouseEventArgs e, float min, float max)
        {
            Hit h = TopAt(e.Location);
            if (h == null) return min;
            return ValueFromBar(e.X, h.Rect, min, max);
        }

        static float ValueFromBar(float x, RectangleF rect, float min, float max)
        {
            float t = (x - rect.X) / rect.Width;
            if (t < 0f) t = 0f;
            if (t > 1f) t = 1f;
            return min + (max - min) * t;
        }

        void ApplyFader(int y)
        {
            Hit h = null;
            foreach (Hit hh in hits) if (hh.Action == "fader" && hh.Index == dragIndex) h = hh;
            if (h == null) return;
            float top = h.Rect.Y + 20f;
            float height = h.Rect.Height - 46f;
            float t = 1f - (y - top) / height;
            if (t < 0f) t = 0f;
            if (t > 1f) t = 1f;
            st.Gains[dragIndex] = (float)Math.Round((t * 24f - 12f) * 2f) / 2f;
            st.ActivePreset = "";
            st.Preamp = Catalog.SuggestedPreamp(st.Gains, st.Sub, st.Bass);
            Commit();
        }

        static float Clamp01(float v) { return Clamp(v, 0f, 1f); }
        static float Clamp(float v, float min, float max) { return v < min ? min : (v > max ? max : v); }

        Hit TopAt(Point pt)
        {
            for (int i = hits.Count - 1; i >= 0; i--)
                if (hits[i].Contains(pt)) return hits[i];
            return null;
        }

        // ------------------------------------------------------------------
        void Commit()
        {
            st.Save();
            Invalidate();
        }

        void ApplyApo()
        {
            string err = apo.Apply(st.Gains, st.Sub, st.Bass, st.Preamp);
            eapoApplied = err == null && apo.HasBlock();
            if (err == null) ShowToast("Equalizer APO'ya uygulandı");
            else ShowToast(err);
            Invalidate();
        }

        void ApplyApoSilently()
        {
            if (!apo.Installed()) return;
            apo.Apply(st.Gains, st.Sub, st.Bass, st.Preamp);
            eapoApplied = apo.HasBlock();
        }

        void LaunchApo(string exe)
        {
            try
            {
                string path = Path.Combine(@"C:\Program Files\EqualizerAPO", exe);
                if (File.Exists(path)) Process.Start(path);
                else ShowToast("Bulunamadı: " + exe);
            }
            catch (Exception ex) { ShowToast(ex.Message); }
        }

        void StartGuard()
        {
            try
            {
                string dir = Path.Combine(Path.GetTempPath(), "Rezofon");
                Directory.CreateDirectory(dir);
                string wav = Path.Combine(dir, "hat.wav");
                if (!File.Exists(wav)) WriteSilentWav(wav, 4);
                guardPlayer = new SoundPlayer(wav);
                guardPlayer.PlayLooping();
            }
            catch { }
        }

        void StopGuard()
        {
            try { if (guardPlayer != null) { guardPlayer.Stop(); guardPlayer.Dispose(); guardPlayer = null; } }
            catch { }
        }

        static void WriteSilentWav(string path, int seconds)
        {
            int rate = 44100;
            int channels = 2;
            int bits = 16;
            int dataLen = rate * channels * (bits / 8) * seconds;
            using (FileStream fs = new FileStream(path, FileMode.Create))
            using (BinaryWriter w = new BinaryWriter(fs))
            {
                w.Write(System.Text.Encoding.ASCII.GetBytes("RIFF"));
                w.Write(36 + dataLen);
                w.Write(System.Text.Encoding.ASCII.GetBytes("WAVE"));
                w.Write(System.Text.Encoding.ASCII.GetBytes("fmt "));
                w.Write(16);
                w.Write((short)1);
                w.Write((short)channels);
                w.Write(rate);
                w.Write(rate * channels * (bits / 8));
                w.Write((short)(channels * (bits / 8)));
                w.Write((short)bits);
                w.Write(System.Text.Encoding.ASCII.GetBytes("data"));
                w.Write(dataLen);
                byte[] zero = new byte[dataLen];
                w.Write(zero);
            }
        }
    }
}
