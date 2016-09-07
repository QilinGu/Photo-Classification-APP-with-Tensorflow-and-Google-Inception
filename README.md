# Photo-Classification-APP-with-Tensorflow-and-Google-Inception

===========================================================================
v 0.5.1

I added 3 more classes, cate, cat and dog, into the classification model. So we have 6 phot categories now.

===========================================================================

v 0.5.0

1. This APP classifies an photo into one of three classes: People, Urban and Nature. Images beyond these catergories (like a photo of a kitten or a soccer game) will get poor prediction;
2. The app was build with google bazel. I donnot know if it can be easily converted to eclipse project;
3. The classification model was fine-tuned on Inception-v3 (Find details in Google's people "Rethinking the Inception Architecture for Computer Vision".) The training images were picked from some public datasets rather arbitrarily, as following:

          totally 4773 images from:
        
          Stanford_UprightInvertedSet >> 720 ALL
        
          Stanford_UprightInvertedSet >> Nature 360;
        
          Stanford_UprightInvertedSet >> Urban 360;
        
          THUS10000 >> 1083 PARTIAL
        
          THUS10000 >> people 1083
        
          ImageNet n09303008 >> 1746 All
        
          ImageNet n09303008 >> Nature 1701
        
          ImageNet n09303008 >> People 45
        
          ImageNet n08524735 >> 1224 All                                                                                               
          
          ImageNet n08524735 >> Urban 1215
          
          ImageNet n08524735 >> People 9
          
          randomly partition to val set and train set  by 1:3
  
4. This is just a toy project to verify android tensorflow. For a more usable APP, the photo catergories should be carefully subdivided & completed and a lot more training images are needed.
