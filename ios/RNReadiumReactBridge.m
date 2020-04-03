
#import "RNReadiumReactBridge.h"

#import "RNReadiumReactBridge-Swift.h"
@implementation RNReadiumReactBridge

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}
RCT_EXPORT_MODULE(RNReadiumReactBridge)

- (NSArray<NSString *> *) supportedEvents
{
    return @[@"pageChanged"];
}
+ (void) staticChange
{
    [instance1 sendEventWithName:@"pageChanged" body:@{@"index": @"0"}];
}

RCT_EXPORT_METHOD(navigateToReadium:(NSString*) string)
{
    instance1 = self;
    printf("navigateToREADIUM \n");
    ActivityStarter *start = [[ActivityStarter alloc] init];
    [start navigateToReadium:string handler:^(int index, int total, NSString* url) {
        [instance1 sendEventWithName:@"pageChanged" body:@{@"index": [NSNumber numberWithInt:index], @"total": [NSNumber numberWithInt:total], @"url": url}];
    }];
}
@end
  
