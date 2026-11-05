using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Text;

namespace Rezofon
{
    internal class Preset
    {
        public string Id;
        public string Name;
        public string Tagline;
        public float[] Gains;
        public float Bass;
        public float Sub;
        public float Loudness;

        public Preset(string id, string name, string tagline, float[] gains, float bass, float sub, float loud)
        {
            Id = id; Name = name; Tagline = tagline; Gains = gains; Bass = bass; Sub = sub; Loudness = loud;
        }
    }

    internal static class Catalog
    {
        public static readonly float[] Freqs = { 31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f };
        public static readonly string[] Labels = { "31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k" };

        public static List<Preset> Presets()
        {
            List<Preset> list = new List<Preset>();
            list.Add(new Preset("jam_balance", "JAM+ Dengesi", "Fabrika dengesi, hafif canlılık",
                new float[] { 4.5f, 3.5f, 2f, 0.5f, -0.5f, 0f, 0.5f, 1f, 1.5f, 2f }, 0.30f, 3f, 0.20f));
            list.Add(new Preset("deep_bass", "Derin Bas", "Pasif radyatörü uyandır",
                new float[] { 7.5f, 6.5f, 4.5f, 1.5f, -1.5f, -1f, 0f, 1f, 1.5f, 2f }, 0.72f, 7f, 0.35f));
            list.Add(new Preset("clean_punch", "Temiz Vuruş", "Bas var, bulanıklık yok",
                new float[] { 2f, 4.5f, 4f, 0f, -2f, -1f, 0.5f, 2f, 2.5f, 2f }, 0.55f, 2f, 0.25f));
            list.Add(new Preset("vocal_front", "Vokal Önde", "Podcast ve sözlü içerik",
                new float[] { -1f, -1.5f, -1f, 0.5f, 1.5f, 2.5f, 3f, 2f, 0.5f, -0.5f }, 0.10f, 0f, 0.15f));
            list.Add(new Preset("open_air", "Açık Hava", "Bahçe ve balkon için parlaklık",
                new float[] { 3f, 3f, 1.5f, 0f, 0.5f, 1f, 2.5f, 3.5f, 4f, 3.5f }, 0.45f, 4f, 0.30f));
            list.Add(new Preset("night", "Gece Modu", "Düşük seansta denge",
                new float[] { 6f, 5f, 3f, 1f, 0.5f, 1f, 1.5f, 1f, 0f, -2f }, 0.40f, 5f, 0.75f));
            list.Add(new Preset("electronic", "Elektronik", "Kick ve bas hattı",
                new float[] { 6f, 5.5f, 3f, -0.5f, -1.5f, 0.5f, 1.5f, 2.5f, 3f, 2.5f }, 0.65f, 6f, 0.30f));
            list.Add(new Preset("rock", "Rock", "Gitar gövdesi ve davul",
                new float[] { 3f, 4f, 3f, 1f, -1f, -0.5f, 1.5f, 3f, 3.5f, 2.5f }, 0.45f, 3f, 0.25f));
            return list;
        }

        public static float Interpolate(float[] gains, float hz)
        {
            if (hz <= Freqs[0]) return gains[0];
            if (hz >= Freqs[Freqs.Length - 1]) return gains[Freqs.Length - 1];
            int i = 0;
            while (i < Freqs.Length - 2 && hz > Freqs[i + 1]) i++;
            double t = (Math.Log10(hz) - Math.Log10(Freqs[i])) /
                       (Math.Log10(Freqs[i + 1]) - Math.Log10(Freqs[i]));
            return (float)(gains[i] + (gains[i + 1] - gains[i]) * t);
        }

        public static float Preview(float[] gains, float hz, float sub, float bass)
        {
            float baseGain = Interpolate(gains, hz);
            float shelf = sub * (1f / (1f + (float)Math.Pow(hz / 95f, 2.4f)));
            float punch = bass * 6f * (1f / (1f + (float)Math.Pow(hz / 170f, 2.0f)));
            return baseGain + shelf + punch;
        }

        public static float SuggestedPreamp(float[] gains, float sub, float bass)
        {
            float peak = 0f;
            for (int i = 0; i < gains.Length; i++) if (gains[i] > peak) peak = gains[i];
            float total = peak + sub * 0.8f + bass * 5f;
            if (total <= 3f) return 0f;
            return -Math.Min(9f, total - 3f);
        }
    }

    internal class State
    {
        public bool Enabled = true;
        public float[] Gains = new float[10];
        public float Bass = 0.30f;
        public bool BassEnabled = true;
        public float Sub = 3f;
        public float Loudness = 0.20f;
        public float Preamp = 0f;
        public bool Guard = false;
        public string ActivePreset = "jam_balance";
        public bool Dark = true;

        public static string FilePath
        {
            get
            {
                string dir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "Rezofon");
                Directory.CreateDirectory(dir);
                return Path.Combine(dir, "state.ini");
            }
        }

        public void Load()
        {
            try
            {
                if (!File.Exists(FilePath)) return;
                string[] lines = File.ReadAllLines(FilePath);
                foreach (string raw in lines)
                {
                    int eq = raw.IndexOf('=');
                    if (eq < 0) continue;
                    string k = raw.Substring(0, eq).Trim();
                    string v = raw.Substring(eq + 1).Trim();
                    if (k == "enabled") Enabled = v == "1";
                    else if (k == "gains")
                    {
                        string[] parts = v.Split(',');
                        for (int i = 0; i < 10 && i < parts.Length; i++)
                        {
                            float f;
                            if (float.TryParse(parts[i], NumberStyles.Float, CultureInfo.InvariantCulture, out f)) Gains[i] = f;
                        }
                    }
                    else if (k == "bass") Bass = Parse(v, Bass);
                    else if (k == "bass_enabled") BassEnabled = v == "1";
                    else if (k == "sub") Sub = Parse(v, Sub);
                    else if (k == "loudness") Loudness = Parse(v, Loudness);
                    else if (k == "preamp") Preamp = Parse(v, Preamp);
                    else if (k == "guard") Guard = v == "1";
                    else if (k == "preset") ActivePreset = v;
                    else if (k == "dark") Dark = v == "1";
                }
            }
            catch { }
        }

        public void Save()
        {
            try
            {
                StringBuilder sb = new StringBuilder();
                sb.AppendLine("enabled=" + (Enabled ? "1" : "0"));
                sb.Append("gains=");
                for (int i = 0; i < 10; i++)
                {
                    if (i > 0) sb.Append(",");
                    sb.Append(Gains[i].ToString("0.0", CultureInfo.InvariantCulture));
                }
                sb.AppendLine();
                sb.AppendLine("bass=" + Bass.ToString("0.000", CultureInfo.InvariantCulture));
                sb.AppendLine("bass_enabled=" + (BassEnabled ? "1" : "0"));
                sb.AppendLine("sub=" + Sub.ToString("0.0", CultureInfo.InvariantCulture));
                sb.AppendLine("loudness=" + Loudness.ToString("0.00", CultureInfo.InvariantCulture));
                sb.AppendLine("preamp=" + Preamp.ToString("0.0", CultureInfo.InvariantCulture));
                sb.AppendLine("guard=" + (Guard ? "1" : "0"));
                sb.AppendLine("preset=" + ActivePreset);
                sb.AppendLine("dark=" + (Dark ? "1" : "0"));
                File.WriteAllText(FilePath, sb.ToString(), Encoding.UTF8);
            }
            catch { }
        }

        static float Parse(string s, float fallback)
        {
            float f;
            if (float.TryParse(s, NumberStyles.Float, CultureInfo.InvariantCulture, out f)) return f;
            return fallback;
        }

        public void ApplyPreset(Preset p)
        {
            for (int i = 0; i < 10; i++) Gains[i] = p.Gains[i];
            Bass = p.Bass;
            Sub = p.Sub;
            Loudness = p.Loudness;
            Preamp = Catalog.SuggestedPreamp(Gains, Sub, Bass);
            ActivePreset = p.Id;
            Enabled = true;
        }

        public string ToJson()
        {
            StringBuilder sb = new StringBuilder();
            sb.AppendLine("{");
            sb.AppendLine("  \"format\": \"rezofon.profile/1\",");
            sb.Append("  \"gains\": [");
            for (int i = 0; i < 10; i++)
            {
                if (i > 0) sb.Append(", ");
                sb.Append(Gains[i].ToString("0.0", CultureInfo.InvariantCulture));
            }
            sb.AppendLine("],");
            sb.AppendLine("  \"bass\": " + Bass.ToString("0.00", CultureInfo.InvariantCulture) + ",");
            sb.AppendLine("  \"sub\": " + Sub.ToString("0.0", CultureInfo.InvariantCulture) + ",");
            sb.AppendLine("  \"loudness\": " + Loudness.ToString("0.00", CultureInfo.InvariantCulture) + ",");
            sb.AppendLine("  \"preamp\": " + Preamp.ToString("0.0", CultureInfo.InvariantCulture) + ",");
            sb.AppendLine("  \"enabled\": " + (Enabled ? "true" : "false"));
            sb.AppendLine("}");
            return sb.ToString();
        }
    }
}
