//
// Created by Mihail Gutan on 10/11/16.
//

#ifndef eactalk_JNIBASE58_H
#define eactalk_JNIBASE58_H

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jstring JNICALL Java_com_jniwrappers_BRBase58_base58Encode(
        JNIEnv *env,
        jobject thiz,
        jbyteArray data);

#ifdef __cplusplus
}
#endif

#endif //eactalk_JNIBASE58_H
