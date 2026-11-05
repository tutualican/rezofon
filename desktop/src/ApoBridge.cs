using System;
using System.Collections.Generic;
using System.IO;
using System.Text;

namespace Rezofon
{
    /// <summary>
    /// Bridges Rezofon to Equalizer APO. Rezofon never rewrites the user's
    /// whole config; it appends one marked include block and writes its own
    /// file, both removable in one click.
    /// </summary>
    internal class ApoBridge
    {
        public const string BeginMarker = "# >>> REZOFON >>>";
        public const string EndMarker = "# <<< REZOFON <<<";
        public const string IncludeFile = "rezofon.txt";

        public string ConfigDir = @"C:\Program Files\EqualizerAPO\config";
        public const string BackupFile = "config.rezofon-backup.txt";
        static readonly UTF8Encoding Utf8 = new UTF8Encoding(false);

        public bool Installed()
        {
            return Directory.Exists(ConfigDir) && File.Exists(Path.Combine(ConfigDir, "config.txt"));
        }

        public string MainConfig { get { return Path.Combine(ConfigDir, "config.txt"); } }
        public string OwnConfig { get { return Path.Combine(ConfigDir, IncludeFile); } }
        public string BackupPath { get { return Path.Combine(ConfigDir, BackupFile); } }

        public bool HasBlock()
        {
            if (!Installed()) return false;
            try { return File.ReadAllText(MainConfig).Contains(BeginMarker); }
            catch { return false; }
        }

        public string Status()
        {
            if (!Installed()) return "Equalizer APO bulunamadı";
            return HasBlock() ? "Etkin · config.txt içine bağlı" : "Kurulu · henüz bağlanmadı";
        }

        /// <summary>Writes the band curve. Returns null on success, else a message.</summary>
        public string Apply(float[] gains, float subShelf, float bass, float preamp)
        {
            try
            {
                if (!Installed()) return "Equalizer APO kurulu değil.";
                Directory.CreateDirectory(ConfigDir);
                StringBuilder sb = new StringBuilder();
                sb.AppendLine("# =====================================================================");
                sb.AppendLine("#  REZOFON tarafindan uretildi - elle duzenlemek yerine uygulamayi kullan");
                sb.AppendLine("#  10 bant + sub raf + bas vurgusu + headroom");
                sb.AppendLine("# =====================================================================");
                sb.AppendLine();
                sb.AppendLine("Preamp: " + F(preamp) + " dB");
                sb.AppendLine();
                sb.AppendLine("# 62 Hz alti: JAM+ surucusu bu bolgeyi uretemez, sadece govde titrer.");
                sb.AppendLine("Filter 1: ON HPQ Fc 62 Hz Q 0.707");
                sb.AppendLine();
                sb.AppendLine("# Bant egrisi");
                float[] freqs = new float[] { 31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f };
                int id = 2;
                for (int i = 0; i < freqs.Length; i++)
                {
                    float g = gains[i];
                    if (i < 3) g += subShelf * (1f - freqs[i] / 250f) + bass * 4f * (1f - freqs[i] / 200f);
                    if (Math.Abs(g) < 0.05f) continue;
                    sb.AppendLine("Filter " + id + ": ON PK Fc " + freqs[i].ToString("0") + " Hz Gain " + F(g) + " dB Q 1.1");
                    id++;
                }
                sb.AppendLine();
                sb.AppendLine("# Konum: " + DateTime.Now.ToString("yyyy-MM-dd HH:mm"));
                File.WriteAllText(OwnConfig, sb.ToString(), Utf8);

                EnsureBlock();
                return null;
            }
            catch (UnauthorizedAccessException)
            {
                return "Yazma izni yok. Rezofon'u yönetici olarak çalıştırın.";
            }
            catch (Exception ex)
            {
                return ex.Message;
            }
        }

        public string Remove()
        {
            try
            {
                if (!Installed()) return "Equalizer APO kurulu değil.";
                string text = File.ReadAllText(MainConfig);
                int a = text.IndexOf(BeginMarker, StringComparison.Ordinal);
                int b = text.IndexOf(EndMarker, StringComparison.Ordinal);
                if (a >= 0 && b > a)
                {
                    int end = b + EndMarker.Length;
                    if (end < text.Length && text[end] == '\r') end++;
                    if (end < text.Length && text[end] == '\n') end++;
                    File.WriteAllText(MainConfig, text.Remove(a, end - a), Utf8);
                }
                if (File.Exists(OwnConfig)) File.Delete(OwnConfig);

                // If a pristine backup exists, put the user's file back byte for byte.
                if (File.Exists(BackupPath))
                {
                    string now = File.ReadAllText(MainConfig);
                    string backup = File.ReadAllText(BackupPath);
                    if (Normalize(now) == Normalize(backup))
                    {
                        File.Copy(BackupPath, MainConfig, true);
                        File.Delete(BackupPath);
                    }
                }
                return null;
            }
            catch (UnauthorizedAccessException)
            {
                return "Yazma izni yok. Yönetici olarak çalıştırın.";
            }
            catch (Exception ex)
            {
                return ex.Message;
            }
        }

        static string Normalize(string s)
        {
            return s.Replace("\r\n", "\n").TrimEnd();
        }

        void EnsureBlock()
        {
            string text = File.ReadAllText(MainConfig);
            if (text.Contains(BeginMarker)) return;
            if (!File.Exists(BackupPath))
            {
                try { File.Copy(MainConfig, BackupPath, true); } catch { }
            }
            StringBuilder sb = new StringBuilder(text);
            if (!text.EndsWith("\n")) sb.AppendLine();
            sb.AppendLine();
            sb.AppendLine(BeginMarker);
            sb.AppendLine("Include: " + IncludeFile);
            sb.AppendLine(EndMarker);
            File.WriteAllText(MainConfig, sb.ToString(), Utf8);
        }

        static string F(float v)
        {
            return v.ToString("0.0", System.Globalization.CultureInfo.InvariantCulture);
        }
    }
}
