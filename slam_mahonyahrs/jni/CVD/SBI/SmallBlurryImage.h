// -*- c++ -*-
// Copyright 2008 Isis Innovation Limited
//
// SmallBlurryImage - A small and blurry representation of an image.
// used by the relocaliser.

#ifndef __SMALLBLURRYIMAGE_H
#define __SMALLBLURRYIMAGE_H
#include <cvd/image.h>
#include <cvd/byte.h>
#include <TooN/se2.h>
#include <TooN/se3.h>
#include "ATANCamera.h"

class SmallBlurryImage
{
 public:
  SmallBlurryImage();
  ~SmallBlurryImage() {}
  SmallBlurryImage(const CVD::Image<CVD::byte>& image, double dBlur = 2.5);
  void MakeFromImg(const CVD::Image<CVD::byte>& image, double dBlur = 2.5);
  void MakeJacs();
  double ZMSSD(SmallBlurryImage &other);
  std::pair<SE2<>,double> IteratePosRelToTarget(SmallBlurryImage &other, int nIterations = 10);
  static SE3<> SE3fromSE2(SE2<> se2, ATANCamera camera);
  // the camera is a problem
  static CVD::ImageRef GetSize();
  
protected:
  CVD::Image<CVD::byte> mimSmall;
  CVD::Image<float> mimTemplate;
  CVD::Image<Vector<2> > mimImageJacs;
  bool mbMadeJacs;
  static CVD::ImageRef mirSize;
};

#endif


