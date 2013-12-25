/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/benjamin/workspace/AIS/src/com/bmw/android/indexservice/BSearchService.aidl
 */
package com.bmw.android.indexservice;
public interface BSearchService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.bmw.android.indexservice.BSearchService
{
private static final java.lang.String DESCRIPTOR = "com.bmw.android.indexservice.BSearchService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.bmw.android.indexservice.BSearchService interface,
 * generating a proxy if needed.
 */
public static com.bmw.android.indexservice.BSearchService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.bmw.android.indexservice.BSearchService))) {
return ((com.bmw.android.indexservice.BSearchService)iin);
}
return new com.bmw.android.indexservice.BSearchService.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_find:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
boolean[] _result = this.find(_arg0);
reply.writeNoException();
reply.writeBooleanArray(_result);
return true;
}
case TRANSACTION_buildIndex:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.util.List<java.lang.String> _arg1;
_arg1 = data.createStringArrayList();
int _arg2;
_arg2 = data.readInt();
int _arg3;
_arg3 = data.readInt();
this.buildIndex(_arg0, _arg1, _arg2, _arg3);
reply.writeNoException();
return true;
}
case TRANSACTION_load:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
boolean _result = this.load(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.bmw.android.indexservice.BSearchService
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
@Override public boolean[] find(java.lang.String text) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean[] _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(text);
mRemote.transact(Stub.TRANSACTION_find, _data, _reply, 0);
_reply.readException();
_result = _reply.createBooleanArray();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void buildIndex(java.lang.String filePath, java.util.List<java.lang.String> text, int page, int maxPage) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(filePath);
_data.writeStringList(text);
_data.writeInt(page);
_data.writeInt(maxPage);
mRemote.transact(Stub.TRANSACTION_buildIndex, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public boolean load(java.lang.String filePath) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(filePath);
mRemote.transact(Stub.TRANSACTION_load, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_find = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_buildIndex = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_load = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
}
public boolean[] find(java.lang.String text) throws android.os.RemoteException;
public void buildIndex(java.lang.String filePath, java.util.List<java.lang.String> text, int page, int maxPage) throws android.os.RemoteException;
public boolean load(java.lang.String filePath) throws android.os.RemoteException;
}
