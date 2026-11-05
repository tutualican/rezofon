using System;
using System.Runtime.InteropServices;
using System.Windows.Forms;

namespace Rezofon
{
    internal static class Program
    {
        [DllImport("shcore.dll")]
        static extern int SetProcessDpiAwareness(int value);

        [STAThread]
        static void Main()
        {
            try { SetProcessDpiAwareness(2); } catch { }
            Application.EnableVisualStyles();            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new RezofonForm());
        }
    }
}
