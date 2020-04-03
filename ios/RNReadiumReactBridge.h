
#if __has_include("RCTBridgeModule.h")
//#import "RCTBridgeModule.h"
#import "RCTEventEmitter.h"
#else
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#endif
@class ActivityStarter;

@interface ActivityStarter : NSObject
- (void) navigateToReadium: (NSString*) str handler: (void(^)(int,int,NSString*))handler;
@end


@interface RNReadiumReactBridge : RCTEventEmitter <RCTBridgeModule>
+ (void) staticChange;
@end
  
static RNReadiumReactBridge* instance1;
