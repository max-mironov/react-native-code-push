#import "CodePush.h"

@interface NSDictionary (UrlEncoding)

-(NSString*) urlEncodedString;

@end

// helper function: get the string form of any object
static NSString *toString(id object) {
    return [NSString stringWithFormat: @"%@", object];
}

// helper function: get the url encoded string form of any object
static NSString *urlEncode(id object) {
    NSString *string = toString(object);
    return [string stringByAddingPercentEscapesUsingEncoding: NSUTF8StringEncoding];
}

@implementation NSDictionary (UrlEncoding)

-(NSString*) urlEncodedString {
    NSMutableArray *parts = [NSMutableArray array];
    for (id key in self) {
        id value = [self objectForKey: key];
        NSString *part = [NSString stringWithFormat: @"%@=%@", urlEncode(key), urlEncode(value)];
        [parts addObject: part];
    }
    return [parts componentsJoinedByString: @"&"];
}

@end




@implementation CodePushAquisitionSDKManager

#pragma mark - Static variables

static NSString *const AppVersionKey = @"appVersion";
static NSString *const ServerURLConfigKey = @"serverUrl";
static NSString *const DeploymentKeyConfigKey = @"deploymentKey";
static NSString *const ClientUniqueIDConfigKey = @"clientUniqueId";
static NSString *const PackageHashKey = @"packageHash";
static NSString *const LabelKey = @"label";
static NSString *const IgonreAppVersionKey = @"_ignoreAppVersion";
static NSString *const UpdateInfoKey = @"updateInfo";
static NSString *const IsCompanionKey = @"isCompanion";
static NSString *const IsAvailableKey = @"isAvailable";
static NSString *const UpdateAppVersionKey = @"updateAppVersion";
static NSString *const DescriptionKey = @"description";
static NSString *const IsMandatoryKey = @"isMandatory";
static NSString *const PackageSizeKey = @"packageSize";
static NSString *const DownloadUrlKey = @"downloadURL";


- (instancetype) initWithConfig:(NSDictionary *)config
{
    self.serverURL = [config objectForKey:ServerURLConfigKey];
    self.appVersion = [config objectForKey:AppVersionKey];
    self.deploymentKey = [config objectForKey:DeploymentKeyConfigKey];
    self.clientUniqueId = [config objectForKey:ClientUniqueIDConfigKey];
    self.ignoreAppVersion = [config objectForKey:IgonreAppVersionKey];
    return self;
}

- (NSDictionary *)queryUpdateWithCurrentPackage:(NSDictionary *)currentPackage
{
    NSDictionary *response = nil;
    if (!currentPackage || ![currentPackage objectForKey:AppVersionKey]){
        CPLog(@"Unable to query update, no package provided or calling with incorrect package");
        return response;
    }
    
    NSDictionary *updateRequest = [[NSDictionary alloc] initWithObjectsAndKeys:
                                   [self deploymentKey ], DeploymentKeyConfigKey,
                                   [self appVersion], AppVersionKey,
                                   [currentPackage objectForKey:PackageHashKey],PackageHashKey,
                                   [self ignoreAppVersion], IsCompanionKey,
                                   [currentPackage objectForKey:LabelKey],LabelKey,
                                   [self clientUniqueId],ClientUniqueIDConfigKey,
                                   nil];
    
    NSString *urlEncodedString = [updateRequest urlEncodedString];
    NSString *requestUrl = [NSString stringWithFormat:@"%@%@%@", [self serverURL], @"updateCheck?", urlEncodedString];
    
    
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] init];
    [request setHTTPMethod:@"GET"];
    [request setURL:[NSURL URLWithString:requestUrl]];
    
    NSError *error = nil;
    NSHTTPURLResponse *responseCode = nil;
    
    NSData *oResponseData = [NSURLConnection sendSynchronousRequest:request returningResponse:&responseCode error:&error];
    
    if([responseCode statusCode] != 200){
        NSLog(@"Error getting %@, HTTP status code %i", requestUrl, [responseCode statusCode]);
        return response;
    }
    
    NSDictionary *dictionaryResponse = [NSJSONSerialization JSONObjectWithData:oResponseData options:0 error:&error];
    
    if (!error && dictionaryResponse && [dictionaryResponse objectForKey:UpdateInfoKey]){
        response = [dictionaryResponse objectForKey:UpdateInfoKey];
    } else {
        return response;
    }
    
    BOOL updateAppVersion = [[response objectForKey:UpdateAppVersionKey]boolValue];
    if (updateAppVersion){
        return @{ UpdateAppVersionKey: @(YES),
                  AppVersionKey:[response objectForKey:AppVersionKey]
                  };
    }
    
    BOOL isAvaliable = [[response objectForKey:IsAvailableKey]boolValue];
    if (isAvaliable == NO){
        return nil;
    }
    
    
    
    NSDictionary *remotePackage = [[NSDictionary alloc] initWithObjectsAndKeys:
                                   [self deploymentKey ],DeploymentKeyConfigKey,
                                   [response objectForKey:DescriptionKey],DescriptionKey,
                                   [response objectForKey:LabelKey],LabelKey,
                                   [response objectForKey:AppVersionKey],AppVersionKey,
                                   [response objectForKey:LabelKey],LabelKey,
                                   [response objectForKey:IsMandatoryKey],IsMandatoryKey,
                                   [response objectForKey:PackageHashKey],PackageHashKey,
                                   [response objectForKey:PackageSizeKey],PackageSizeKey,
                                   [response objectForKey:DownloadUrlKey],DownloadUrlKey,
                                   nil];
    
    
    return remotePackage;
}

- (NSDictionary *)reportStatusDeploy:(NSDictionary *)package
                          withStatus:(NSString *)status
           previousLabelOrAppVersion:(NSString *)prevLabelOrAppVersion
               previousDeploymentKey:(NSString *)prevDeploymentKey
{
    @throw [NSException exceptionWithName:NSInternalInconsistencyException
                                   reason:[NSString stringWithFormat:@"You must implement %@", NSStringFromSelector(_cmd)]
                                 userInfo:nil];
}

- (NSDictionary *)reportStatusDownload:(NSDictionary *)downloadedPackage
{
    @throw [NSException exceptionWithName:NSInternalInconsistencyException
                                   reason:[NSString stringWithFormat:@"You must implement %@", NSStringFromSelector(_cmd)]
                                 userInfo:nil];
}

@end
