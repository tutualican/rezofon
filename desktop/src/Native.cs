using System;
using System.Runtime.InteropServices;

namespace Rezofon
{
    // ---- Core Audio (MMDevice / IAudioEndpointVolume) --------------------
    [ComImport, Guid("BCDE0395-E52F-467C-8E3D-C4579291692E")]
    internal class MMDeviceEnumeratorComObject { }

    internal enum EDataFlow { eRender = 0, eCapture = 1, eAll = 2 }
    internal enum ERole { eConsole = 0, eMultimedia = 1, eCommunications = 2 }

    internal enum DEVICE_STATE : uint
    {
        ACTIVE = 0x00000001,
        DISABLED = 0x00000002,
        NOTPRESENT = 0x00000004,
        UNPLUGGED = 0x00000008,
        ALL = 0x0000000F
    }

    [ComImport, Guid("A95664D2-9614-4F35-A746-DE8DB63617E6"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    internal interface IMMDeviceEnumerator
    {
        int EnumAudioEndpoints(EDataFlow dataFlow, DEVICE_STATE dwStateMask, out IMMDeviceCollection ppDevices);
        int GetDefaultAudioEndpoint(EDataFlow dataFlow, ERole role, out IMMDevice ppDevice);
        int GetDevice(string pwstrId, out IMMDevice ppDevice);
        int RegisterEndpointNotificationCallback(IntPtr pClient);
        int UnregisterEndpointNotificationCallback(IntPtr pClient);
    }

    [ComImport, Guid("0BD7A1BE-7A1A-44DB-8397-CC5392387B5E"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    internal interface IMMDeviceCollection
    {
        int GetCount(out int pcDevices);
        int Item(int nDevice, out IMMDevice ppDevice);
    }

    [ComImport, Guid("D666063F-1587-4E43-81F1-B948E807363F"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    internal interface IMMDevice
    {
        int Activate(ref Guid iid, int dwClsCtx, IntPtr pActivationParams,
            [MarshalAs(UnmanagedType.IUnknown)] out object ppInterface);
        int OpenPropertyStore(int stgmAccess, out IPropertyStore ppProperties);
        int GetId([MarshalAs(UnmanagedType.LPWStr)] out string ppstrId);
        int GetState(out DEVICE_STATE pdwState);
    }

    [ComImport, Guid("886d8eeb-8cf2-4446-8d02-cdba1dbdcf99"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    internal interface IPropertyStore
    {
        int GetCount(out int cProps);
        int GetAt(int iProp, out PROPERTYKEY pkey);
        int GetValue(ref PROPERTYKEY key, out PROPVARIANT pv);
        int SetValue(ref PROPERTYKEY key, ref PROPVARIANT propvar);
        int Commit();
    }

    [StructLayout(LayoutKind.Sequential)]
    internal struct PROPERTYKEY
    {
        public Guid fmtid;
        public int pid;
    }

    [StructLayout(LayoutKind.Explicit)]
    internal struct PROPVARIANT
    {
        [FieldOffset(0)] public short vt;
        [FieldOffset(8)] public IntPtr pointerValue;
    }

    [ComImport, Guid("5CDF2C82-841E-4546-9722-0CF74078229A"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    internal interface IAudioEndpointVolume
    {
        int RegisterControlChangeNotify(IntPtr pNotify);
        int UnregisterControlChangeNotify(IntPtr pNotify);
        int GetChannelCount(out uint pnChannelCount);
        int SetMasterVolumeLevel(float fLevelDB, Guid pguidEventContext);
        int SetMasterVolumeLevelScalar(float fLevel, Guid pguidEventContext);
        int GetMasterVolumeLevel(out float pfLevelDB);
        int GetMasterVolumeLevelScalar(out float pfLevel);
        int SetChannelVolumeLevel(uint nChannel, float fLevelDB, Guid pguidEventContext);
        int SetChannelVolumeLevelScalar(uint nChannel, float fLevel, Guid pguidEventContext);
        int GetChannelVolumeLevel(uint nChannel, out float pfLevelDB);
        int GetChannelVolumeLevelScalar(uint nChannel, out float pfLevel);
        int SetMute([MarshalAs(UnmanagedType.Bool)] bool bMute, Guid pguidEventContext);
        int GetMute(out bool pbMute);
        int GetVolumeStepInfo(out uint pnStep, out uint pnStepCount);
        int VolumeStepUp(Guid pguidEventContext);
        int VolumeStepDown(Guid pguidEventContext);
        int QueryHardwareSupport(out uint pdwHardwareSupportMask);
        int GetVolumeRange(out float pflVolumeMindB, out float pflVolumeMaxdB, out float pflVolumeIncrementdB);
    }

    internal class EndpointInfo
    {
        public string Name = "";
        public string Id = "";
        public bool Active;
        public bool IsDefault;
    }

    internal class Audio
    {
        static PROPERTYKEY PKEY_Device_FriendlyName = new PROPERTYKEY
        {
            fmtid = new Guid("a45c254e-df1c-4efd-8020-67d146a850e0"),
            pid = 14
        };

        static IMMDeviceEnumerator Enumerator()
        {
            return (IMMDeviceEnumerator)(new MMDeviceEnumeratorComObject());
        }

        public static EndpointInfo DefaultRender()
        {
            try
            {
                IMMDeviceEnumerator en = Enumerator();
                IMMDevice dev;
                if (en.GetDefaultAudioEndpoint(EDataFlow.eRender, ERole.eMultimedia, out dev) != 0) return null;
                return Describe(dev, true);
            }
            catch { return null; }
        }

        static EndpointInfo Describe(IMMDevice dev, bool isDefault)
        {
            EndpointInfo info = new EndpointInfo();
            info.IsDefault = isDefault;
            try
            {
                string id;
                dev.GetId(out id);
                info.Id = id;
            }
            catch { }
            try
            {
                IPropertyStore store;
                dev.OpenPropertyStore(0, out store);
                if (store != null)
                {
                    PROPVARIANT pv;
                    PROPERTYKEY key = PKEY_Device_FriendlyName;
                    if (store.GetValue(ref key, out pv) == 0 && pv.pointerValue != IntPtr.Zero)
                    {
                        info.Name = Marshal.PtrToStringUni(pv.pointerValue);
                        Marshal.FreeCoTaskMem(pv.pointerValue);
                    }
                }
            }
            catch { }
            try
            {
                DEVICE_STATE st;
                dev.GetState(out st);
                info.Active = st == DEVICE_STATE.ACTIVE;
            }
            catch { }
            return info;
        }

        public static System.Collections.Generic.List<EndpointInfo> RenderEndpoints()
        {
            System.Collections.Generic.List<EndpointInfo> list = new System.Collections.Generic.List<EndpointInfo>();
            try
            {
                IMMDeviceEnumerator en = Enumerator();
                IMMDeviceCollection col;
                en.EnumAudioEndpoints(EDataFlow.eRender, DEVICE_STATE.ALL, out col);
                int count;
                col.GetCount(out count);
                string defId = null;
                try
                {
                    IMMDevice def;
                    en.GetDefaultAudioEndpoint(EDataFlow.eRender, ERole.eMultimedia, out def);
                    def.GetId(out defId);
                }
                catch { }
                for (int i = 0; i < count; i++)
                {
                    IMMDevice d;
                    if (col.Item(i, out d) != 0) continue;
                    EndpointInfo info = Describe(d, false);
                    info.IsDefault = defId != null && defId == info.Id;
                    list.Add(info);
                }
            }
            catch { }
            return list;
        }

        static IAudioEndpointVolume Vol()
        {
            IMMDeviceEnumerator en = Enumerator();
            IMMDevice dev;
            en.GetDefaultAudioEndpoint(EDataFlow.eRender, ERole.eMultimedia, out dev);
            Guid iid = typeof(IAudioEndpointVolume).GUID;
            object o;
            dev.Activate(ref iid, 23, IntPtr.Zero, out o); // CLSCTX_ALL
            return (IAudioEndpointVolume)o;
        }

        public static float GetVolume()
        {
            try { float v; Vol().GetMasterVolumeLevelScalar(out v); return v; }
            catch { return 0.5f; }
        }

        public static void SetVolume(float v)
        {
            try { Vol().SetMasterVolumeLevelScalar(Math.Max(0f, Math.Min(1f, v)), Guid.Empty); }
            catch { }
        }

        public static bool GetMute()
        {
            try { bool m; Vol().GetMute(out m); return m; }
            catch { return false; }
        }

        public static void SetMute(bool m)
        {
            try { Vol().SetMute(m, Guid.Empty); }
            catch { }
        }
    }
}
