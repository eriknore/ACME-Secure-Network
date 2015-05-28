#!/usr/bin/env python2.7

import interfaces_pb2
import random
import time
import base64
import xmlrpclib
import os
import ssl
import sys

REQ_VOUCHER_VEHICLE_TO_LTCA_USING_PROTO_BUFF = 120
RES_VOUCHER_LTCA_TO_VEHICLE_USING_PROTO_BUFF = 121
REQ_X509_CERT_REQ_VEHICLE_TO_LTCA_USING_PROTO_BUFF = 122
RES_ISSUE_X509_CERT_LTCA_TO_VEHICLE_USING_PROTO_BUFF = 123
REQ_X509_CERT_VALIDATION_VEHICLE_TO_LTCA_USING_PROTO_BUFF = 124
RES_X509_CERT_VALIDATION_LTCA_TO_VEHICLE_USING_PROTO_BUFF = 125
REQ_NATIVE_TICKET_VEHICLE_TO_LTCA_USING_PROTO_BUFF = 126
RES_NATIVE_TICKET_LTCA_TO_VEHICLE_USING_PROTO_BUFF = 127
REQ_FOREIGN_TICKET_VEHICLE_TO_LTCA_USING_PROTO_BUFF = 128
RES_FOREIGN_TICKET_LTCA_TO_VEHICLE_USING_PROTO_BUFF = 129

LTCA_SERVER_URL = "https://172.31.212.119/cgi-bin/ltca"

PKI = '/etc/pki'
#PKI = '.'


def init_variables():
    nonce = random.randrange(0, 65535)
    timestamp = int(time.time())
    return nonce, timestamp


def check(req, res, cert):
    if req.iNonce != (res.iNonce - 1):
        return False
    if req.tTimeStamp >= res.tTimeStamp:
        return False
    if cert == res.stSigner.strCertificate:
        return False
    if cert == res.stSigner.strCertificatesChain:
        return False
    if res.stErrInfo.strErrMsgDes != 'NO_ERROR':
        return False
    return True


def load_cert(username):
    try:
        f = open(os.path.join(PKI, 'certs', username + '.csr'),
                 'r')
        csr = f.read()
        f.close()
        f = open(os.path.join(PKI, 'external', 'ltca', 'ltca_x509_cert.arm'),
                 'r')
        cert = f.read()
        f.close()
        f = open(os.path.join(PKI, 'external', 'rca', 'rca_x509_cert.pem'),
                 'r')
        chain = f.read()
        f.close()
    except:
        print("Error, failed at loading certs.")
        sys.exit()
    return csr, cert, chain


def write_cert(username, cert):
    try:
        f = open(os.path.join(PKI, 'cert', username + '-xpki.pem'), 'w')
        f.write(cert)
        return True
    except:
        print("Error, failed at writing cert")
        sys.exit()


def voucher(email):
    req = interfaces_pb2.msgVoucherReq_V2LTCA()
    res = interfaces_pb2.msgVoucherRes_LTCA2V()
    nonce, timestamp = init_variables()
    req.iReqType = REQ_VOUCHER_VEHICLE_TO_LTCA_USING_PROTO_BUFF
    req.strUserName = ''
    req.strPwd = ''
    req.strEmailAddress = email
    req.strCaptcha = 'captcha'
    req.iNonce, req.tTimeStamp = init_variables()
    return req, res


def x509cert(voucher, csr):
    req = interfaces_pb2.msgX509CertReq_V2LTCA()
    res = interfaces_pb2.msgX509CertRes_LTCA2V()
    req.iReqType = REQ_X509_CERT_REQ_VEHICLE_TO_LTCA_USING_PROTO_BUFF
    req.iLTCAIdRange = 1002
    req.strProofOfPossessionVoucher = voucher
    req.strX509CertReq = csr
    req.iNonce, req.tTimeStamp = init_variables()
    return req, res


def connection(req, res):
    context = ssl.SSLContext(ssl.PROTOCOL_TLSv1)
    context.verify_mode = ssl.CERT_NONE
    context.check_hostname = False
    context.load_default_certs()
    proxy = xmlrpclib.ServerProxy(LTCA_SERVER_URL, context=context)
    # Serialize
    sreq = req.SerializeToString()
    # base64 encode
    bsreq = base64.b64encode(sreq)
    try:
        rawdata = proxy.ltca.operate(req.iReqType, bsreq)
        res.ParseFromString(base64.b64decode(rawdata))
        return res
    except:
        print("Error, failed at xmlrpc")
        print(req)
        print(proxy)


def main():
    # Init
    if len(sys.argv) < 2:
        username = raw_input('Type your username: ')
    else:
        username = sys.argv[1]
    email = raw_input('Type your email: ')
    csr, cert, chain = load_cert(username)
    # Cert
    req, res = voucher(email)
    res = connection(req, res)
    if res.iReqType != RES_VOUCHER_LTCA_TO_VEHICLE_USING_PROTO_BUFF:
        print("Error, failed at %s" % req.iReqType)
    if not check(req, res, cert):
        print("Error, check failed")
        print(res)
    # Cert
    voucherkey = raw_input('Paste your voucher from your email: ')
    req, res = x509cert(voucherkey, csr)
    res = connection(req, res)
    if res.iReqType != RES_ISSUE_X509_CERT_LTCA_TO_VEHICLE_USING_PROTO_BUFF:
        print("Error, failed at %s" % req.iReqType)
    if not check(req, res, cert):
        print("Error, check failed")
        print(res)

if __name__ == "__main__":
    main()
