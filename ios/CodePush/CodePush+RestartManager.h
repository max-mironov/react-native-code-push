#import "CodePush.h"

@interface CodePush (RestartManager)

-(void) allowRestart;
-(void) disallowRestart;
-(void) clearPendingRestart;
-(void) restartApp:(BOOL)onlyIfUpdateIsPending;

@end
