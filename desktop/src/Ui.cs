using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Text;
using System.IO;
using System.Windows.Forms;

namespace Rezofon
{
    internal class Palette
    {
        public Color Bg = Color.FromArgb(8, 9, 11);
        public Color Surface = Color.FromArgb(17, 19, 26);
        public Color SurfaceAlt = Color.FromArgb(25, 28, 36);
        public Color Sunken = Color.FromArgb(12, 14, 19);
        public Color Outline = Color.FromArgb(31, 255, 255, 255);
        public Color Lime = Color.FromArgb(200, 255, 61);
        public Color Amber = Color.FromArgb(255, 176, 58);
        public Color Cyan = Color.FromArgb(123, 232, 220);
        public Color Violet = Color.FromArgb(188, 168, 255);
        public Color Text = Color.FromArgb(241, 243, 236);
        public Color Dim = Color.FromArgb(154, 161, 170);
        public Color Danger = Color.FromArgb(255, 107, 94);
        public Color Ink = Color.FromArgb(20, 22, 15);
    }

    internal static class Fonts
    {
        public static FontFamily Display = new FontFamily("Segoe UI");
        public static FontFamily Body = new FontFamily("Segoe UI");

        public static void Load(string dir)
        {
            try
            {
                PrivateFontCollection pfc = new PrivateFontCollection();
                string a = Path.Combine(dir, "fonts", "space_grotesk.ttf");
                string b = Path.Combine(dir, "fonts", "manrope.ttf");
                if (File.Exists(a)) pfc.AddFontFile(a);
                if (File.Exists(b)) pfc.AddFontFile(b);
                if (pfc.Families.Length > 0) Display = pfc.Families[0];
                if (pfc.Families.Length > 1) Body = pfc.Families[1];
            }
            catch { }
        }

        public static Font D(float size, FontStyle style) { return new Font(Display, size, style, GraphicsUnit.Pixel); }
        public static Font B(float size, FontStyle style) { return new Font(Body, size, style, GraphicsUnit.Pixel); }
    }

    internal class Hit
    {
        public RectangleF Rect;
        public string Action;
        public int Index;
        public Hit(RectangleF r, string action) { Rect = r; Action = action; Index = -1; }
        public Hit(RectangleF r, string action, int index) { Rect = r; Action = action; Index = index; }
        public bool Contains(Point p) { return Rect.Contains(p); }
    }

    internal class Spring
    {
        public float Value;
        public float Target;
        float velocity;

        public Spring(float value) { Value = value; Target = value; }

        public bool Step(float dt)
        {
            float stiffness = 520f;
            float damping = 32f;
            float force = (Target - Value) * stiffness - velocity * damping;
            velocity += force * dt;
            Value += velocity * dt;
            if (Math.Abs(Target - Value) < 0.0015f && Math.Abs(velocity) < 0.01f)
            {
                Value = Target;
                velocity = 0f;
                return false;
            }
            return true;
        }
    }

    internal static class Draw
    {
        public static GraphicsPath Round(RectangleF r, float radius)
        {
            GraphicsPath p = new GraphicsPath();
            float d = radius * 2f;
            if (d > r.Width) d = r.Width;
            if (d > r.Height) d = r.Height;
            p.AddArc(r.X, r.Y, d, d, 180, 90);
            p.AddArc(r.Right - d, r.Y, d, d, 270, 90);
            p.AddArc(r.Right - d, r.Bottom - d, d, d, 0, 90);
            p.AddArc(r.X, r.Bottom - d, d, d, 90, 90);
            p.CloseFigure();
            return p;
        }

        public static void Text(Graphics g, string text, Font font, Color color, float x, float y)
        {
            TextRenderer.DrawText(g, text, font, new Point((int)x, (int)y), color,
                TextFormatFlags.NoPadding | TextFormatFlags.NoPrefix | TextFormatFlags.SingleLine);
        }

        public static void Text(Graphics g, string text, Font font, Color color, RectangleF box, bool wrap)
        {
            TextFormatFlags flags = TextFormatFlags.NoPadding | TextFormatFlags.NoPrefix | TextFormatFlags.WordBreak;
            if (!wrap) flags = TextFormatFlags.NoPadding | TextFormatFlags.NoPrefix | TextFormatFlags.SingleLine;
            TextRenderer.DrawText(g, text, font, Rectangle.Round(box), color, flags);
        }

        public static SizeF Measure(Graphics g, string text, Font font)
        {
            return TextRenderer.MeasureText(g, text, font,
                new Size(4000, 4000), TextFormatFlags.NoPadding | TextFormatFlags.NoPrefix | TextFormatFlags.SingleLine);
        }

        public static void Tile(Graphics g, Palette p, RectangleF r, Color? tint, float press, bool hover)
        {
            g.SmoothingMode = SmoothingMode.AntiAlias;
            GraphicsPath path = Round(r, 26f);
            Color top = tint.HasValue ? Blend(p.Surface, tint.Value, 0.16f) : p.Surface;
            Color bottom = tint.HasValue ? Blend(p.Surface, tint.Value, 0.05f) : p.Surface;

            using (LinearGradientBrush b = new LinearGradientBrush(
                new RectangleF(r.X, r.Y, r.Width, r.Height + 1f), top, bottom, 62f))
            {
                g.FillPath(b, path);
            }
            if (tint.HasValue)
            {
                using (GraphicsPath glow = Round(new RectangleF(r.X, r.Y, r.Width, r.Height * 0.55f), 26f))
                using (SolidBrush gb = new SolidBrush(Color.FromArgb(hover ? 26 : 16, tint.Value)))
                {
                    g.FillPath(gb, glow);
                }
            }
            using (Pen pen = new Pen(hover ? Color.FromArgb(46, 255, 255, 255) : p.Outline, 1f))
            {
                g.DrawPath(pen, path);
            }
            if (press < 0.999f)
            {
                using (SolidBrush dim = new SolidBrush(Color.FromArgb((int)((1f - press) * 30), 0, 0, 0)))
                {
                    g.FillPath(dim, path);
                }
            }
            path.Dispose();
        }

        public static Color Blend(Color a, Color b, float t)
        {
            return Color.FromArgb(
                (int)(a.A + (b.A - a.A) * t),
                (int)(a.R + (b.R - a.R) * t),
                (int)(a.G + (b.G - a.G) * t),
                (int)(a.B + (b.B - a.B) * t));
        }

        public static Color Alpha(Color c, int alpha)
        {
            return Color.FromArgb(alpha, c.R, c.G, c.B);
        }

        public static void Curve(Graphics g, RectangleF r, float[] gains, float sub, float bass, Palette p,
            Color accent, Color accent2, bool labels)
        {
            g.SmoothingMode = SmoothingMode.AntiAlias;
            int n = 96;
            PointF[] pts = new PointF[n];
            float pad = 10f;
            float usable = r.Height - pad * 2f;
            for (int i = 0; i < n; i++)
            {
                float t = i / (float)(n - 1);
                float hz = 20f * (float)Math.Pow(20000f / 20f, t);
                float db = Catalog.Preview(gains, hz, sub, bass);
                pts[i] = new PointF(r.X + t * r.Width, r.Y + pad + (12f - db) / 24f * usable);
            }

            for (int i = 0; i <= 4; i++)
            {
                float y = r.Y + pad + usable * i / 4f;
                using (Pen pen = new Pen(Color.FromArgb(p.Outline.A / 2, p.Outline), 1f))
                    g.DrawLine(pen, r.X, y, r.Right, y);
            }
            float zeroY = r.Y + pad + 0.5f * usable;
            using (Pen pen = new Pen(Color.FromArgb(56, p.Dim), 1f))
                g.DrawLine(pen, r.X, zeroY, r.Right, zeroY);

            GraphicsPath fill = new GraphicsPath();
            fill.AddLines(pts);
            fill.AddLine(r.Right, r.Bottom, r.Left, r.Bottom);
            fill.CloseFigure();
            using (LinearGradientBrush lb = new LinearGradientBrush(
                new RectangleF(r.X, r.Y, r.Width, r.Height + 1f),
                Color.FromArgb(76, accent), Color.FromArgb(4, accent), 90f))
            {
                g.FillPath(lb, fill);
            }
            fill.Dispose();

            GraphicsPath line = new GraphicsPath();
            line.AddLines(pts);
            using (LinearGradientBrush lb = new LinearGradientBrush(
                new RectangleF(r.X, r.Y, Math.Max(1f, r.Width), Math.Max(1f, r.Height)), accent2, accent, 0f))
            using (Pen pen = new Pen(lb, 2.4f))
            {
                pen.LineJoin = LineJoin.Round;
                pen.StartCap = LineCap.Round;
                pen.EndCap = LineCap.Round;
                g.DrawPath(pen, line);
            }
            line.Dispose();

            if (labels)
            {
                g.SmoothingMode = SmoothingMode.AntiAlias;
                for (int i = 0; i < 10; i++)
                {
                    float x = r.X + r.Width * (i + 0.5f) / 10f;
                    float db = Catalog.Preview(gains, Catalog.Freqs[i], sub, bass);
                    float y = r.Y + pad + (12f - db) / 24f * usable;
                    using (SolidBrush b = new SolidBrush(accent))
                        g.FillEllipse(b, x - 4.2f, y - 4.2f, 8.4f, 8.4f);
                    using (SolidBrush b = new SolidBrush(p.Bg))
                        g.FillEllipse(b, x - 1.8f, y - 1.8f, 3.6f, 3.6f);
                }
            }
        }

        public static void Icon(Graphics g, string kind, RectangleF r, Color c)
        {
            g.SmoothingMode = SmoothingMode.AntiAlias;
            using (Pen pen = new Pen(c, Math.Max(1.6f, r.Width / 11f)))
            {
                pen.StartCap = LineCap.Round;
                pen.EndCap = LineCap.Round;
                pen.LineJoin = LineJoin.Round;
                float cx = r.X + r.Width / 2f;
                float cy = r.Y + r.Height / 2f;
                float s = r.Width;
                if (kind == "home")
                {
                    float q = s * 0.34f;
                    float gap = s * 0.08f;
                    using (SolidBrush b = new SolidBrush(c))
                    {
                        g.FillRectangle(b, cx - q - gap / 2, cy - q - gap / 2, q, q);
                        g.FillRectangle(b, cx + gap / 2, cy - q - gap / 2, q, q);
                        g.FillRectangle(b, cx - q - gap / 2, cy + gap / 2, q, q);
                        g.FillRectangle(b, cx + gap / 2, cy + gap / 2, q, q);
                    }
                }
                else if (kind == "eq")
                {
                    for (int i = 0; i < 3; i++)
                    {
                        float x = cx + (i - 1) * s * 0.26f;
                        g.DrawLine(pen, x, cy - s * 0.38f, x, cy + s * 0.38f);
                        float knobY = cy + (i == 0 ? -s * 0.16f : i == 1 ? s * 0.12f : -s * 0.03f);
                        using (SolidBrush b = new SolidBrush(c)) g.FillEllipse(b, x - 3.4f, knobY - 3.4f, 6.8f, 6.8f);
                    }
                }
                else if (kind == "bass")
                {
                    for (int i = 0; i < 4; i++)
                    {
                        float x = cx + (i - 1.5f) * s * 0.17f;
                        float h = s * (i == 1 || i == 2 ? 0.5f : 0.26f);
                        using (SolidBrush b = new SolidBrush(c))
                            g.FillRectangle(b, x - s * 0.045f, cy - h / 2f, s * 0.09f, h);
                    }
                }
                else if (kind == "device")
                {
                    PointF a = new PointF(cx - s * 0.1f, cy - s * 0.38f);
                    PointF b = new PointF(cx + s * 0.24f, cy - s * 0.1f);
                    PointF d = new PointF(cx - s * 0.1f, cy + s * 0.38f);
                    PointF e = new PointF(cx + s * 0.24f, cy + s * 0.1f);
                    PointF mid = new PointF(cx - s * 0.1f, cy);
                    g.DrawLine(pen, a, b);
                    g.DrawLine(pen, b, mid);
                    g.DrawLine(pen, mid, d);
                    g.DrawLine(pen, d, e);
                    g.DrawLine(pen, e, mid);
                }
                else if (kind == "tool")
                {
                    g.DrawEllipse(pen, cx - s * 0.34f, cy - s * 0.34f, s * 0.4f, s * 0.4f);
                    g.DrawLine(pen, cx + s * 0.02f, cy + s * 0.02f, cx + s * 0.34f, cy + s * 0.34f);
                }
                else if (kind == "timer")
                {
                    g.DrawEllipse(pen, cx - s * 0.32f, cy - s * 0.28f, s * 0.64f, s * 0.64f);
                    g.DrawLine(pen, cx, cy - s * 0.06f, cx, cy - s * 0.24f);
                    g.DrawLine(pen, cx, cy - s * 0.06f, cx + s * 0.14f, cy + s * 0.02f);
                }
                else if (kind == "gear")
                {
                    g.DrawEllipse(pen, cx - s * 0.16f, cy - s * 0.16f, s * 0.32f, s * 0.32f);
                    for (int i = 0; i < 6; i++)
                    {
                        double ang = i * Math.PI / 3;
                        g.DrawLine(pen,
                            new PointF(cx + (float)Math.Cos(ang) * s * 0.2f, cy + (float)Math.Sin(ang) * s * 0.2f),
                            new PointF(cx + (float)Math.Cos(ang) * s * 0.34f, cy + (float)Math.Sin(ang) * s * 0.34f));
                    }
                }
                else if (kind == "back")
                {
                    g.DrawLine(pen, cx + s * 0.18f, cy - s * 0.26f, cx - s * 0.16f, cy);
                    g.DrawLine(pen, cx - s * 0.16f, cy, cx + s * 0.18f, cy + s * 0.26f);
                }
                else if (kind == "refresh")
                {
                    g.DrawArc(pen, cx - s * 0.3f, cy - s * 0.3f, s * 0.6f, s * 0.6f, 30, 290);
                    using (SolidBrush b = new SolidBrush(c))
                        g.FillPolygon(b, new PointF[] {
                            new PointF(cx + s * 0.3f, cy - s * 0.3f),
                            new PointF(cx + s * 0.36f, cy - s * 0.06f),
                            new PointF(cx + s * 0.1f, cy - s * 0.13f) });
                }
                else if (kind == "power")
                {
                    g.DrawArc(pen, cx - s * 0.3f, cy - s * 0.26f, s * 0.6f, s * 0.6f, -60, 300);
                    g.DrawLine(pen, cx, cy - s * 0.4f, cx, cy - s * 0.06f);
                }
                else if (kind == "volume")
                {
                    using (SolidBrush b = new SolidBrush(c))
                    {
                        g.FillPolygon(b, new PointF[] {
                            new PointF(cx - s * 0.3f, cy - s * 0.12f),
                            new PointF(cx - s * 0.14f, cy - s * 0.12f),
                            new PointF(cx + s * 0.04f, cy - s * 0.3f),
                            new PointF(cx + s * 0.04f, cy + s * 0.3f),
                            new PointF(cx - s * 0.14f, cy + s * 0.12f),
                            new PointF(cx - s * 0.3f, cy + s * 0.12f) });
                    }
                    g.DrawArc(pen, cx - s * 0.02f, cy - s * 0.18f, s * 0.3f, s * 0.36f, -55, 110);
                    g.DrawArc(pen, cx - s * 0.06f, cy - s * 0.3f, s * 0.48f, s * 0.6f, -55, 110);
                }
            }
        }
    }
}
