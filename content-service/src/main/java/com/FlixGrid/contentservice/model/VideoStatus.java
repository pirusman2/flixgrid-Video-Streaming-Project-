package com.FlixGrid.contentservice.model;


// TRACKS THE VIDEO PROCESSING LIFECYCLE

//FLOW:
// PENDING -> UPLOADED -> ENCODING -> ENCODED -> READY
//                  ELSE ->        FAILED


public enum VideoStatus {

    PENDING, // movie added in DB but not uploaded yet
    UPLOADED, // raw video uploaded to S3
    ENCODING, // FFmpeg is encoding the video (converting to chunks)
    ENCODED, // encoding complete
    READY,   // HLS playlist ready  (playlist of chunks) can be streamed
    FAILED   // Encoding failed
}
