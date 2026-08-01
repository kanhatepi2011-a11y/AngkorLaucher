package com.movtery.angkorlauncher.feature.notice

fun interface CheckNoticeListener {
    fun onSuccessful(noticeInfo: NoticeInfo?)
}