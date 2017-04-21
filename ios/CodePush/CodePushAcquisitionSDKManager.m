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

// These constants represent valid deployment statuses
static NSString *const DeploymentFailed = @"DeploymentFailed";
static NSString *const DeploymentSucceeded = @"DeploymentSucceeded";

static NSString *const StatusKey = @"status";
static NSString *const PreviousDeploymentKey = @"previousDeploymentKey";
static NSString *const PreviousLabelOrAppVersionKey = @"previousLabelOrAppVersion";
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
static NSString *const DownloadUrRemotePackageKey = @"downloadUrl"; //there is a mismatch in this, so service require to send with lower case for Url substring


- (instancetype) initWithConfig:(NSDictionary *)config
{
    self.serverURL = [config objectForKey:ServerURLConfigKey];
    self.appVersion = [config objectForKey:AppVersionKey];
    self.deploymentKey = [config objectForKey:DeploymentKeyConfigKey];
    self.clientUniqueId = [config objectForKey:ClientUniqueIDConfigKey];
    if ([config objectForKey:IgonreAppVersionKey]){
        self.ignoreAppVersion = @"YES";
    } else{
        self.ignoreAppVersion = @"NO";
    }

    return self;
}

//TODO: replace this two methods beolw with new methods using NSURLSession plus add ability to use callbacks. Perhaps replace with httpRequester class
+ (NSData *)peformHTTPPostRequest:(NSString *)requestUrl
                   withBody:(NSData *)body
{

    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] init];
    [request setHTTPMethod:@"POST"];
    [request setHTTPBody:body];
    [request setValue:[NSString stringWithFormat:@"%lu", (unsigned long)[body length]] forHTTPHeaderField:@"Content-Length"];
    [request setURL:[NSURL URLWithString:requestUrl]];

    NSError *error = nil;
    NSHTTPURLResponse *responseCode = nil;

    NSData *oResponseData = [NSURLConnection sendSynchronousRequest:request returningResponse:&responseCode error:&error];

    if([responseCode statusCode] != 200){
        NSLog(@"Error getting %@, HTTP status code %li", requestUrl, (long)[responseCode statusCode]);
        return nil;
    }

    return oResponseData;
}

+ (NSData *)peformHTTPGetRequest:(NSString *)requestUrl
{
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] init];
    [request setHTTPMethod:@"GET"];
    [request setURL:[NSURL URLWithString:requestUrl]];
    [request setCachePolicy:NSURLRequestReloadIgnoringCacheData];

    NSError *error = nil;
    NSHTTPURLResponse *responseCode = nil;

    NSData *oResponseData = [NSURLConnection sendSynchronousRequest:request returningResponse:&responseCode error:&error];

    if([responseCode statusCode] != 200){
        NSLog(@"Error getting %@, HTTP status code %li", requestUrl, (long)[responseCode statusCode]);
        return nil;
    }

    return oResponseData;
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

    NSData *oResponseData = [[self class] peformHTTPGetRequest:requestUrl];
    if (oResponseData){
        NSError *error;
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
                                       [response objectForKey:DownloadUrlKey],DownloadUrRemotePackageKey,
                                       nil];

        return remotePackage;
    } else {
        return nil;
    }
}


- (void)reportStatusDeploy:(NSDictionary *)package
                          withStatus:(NSString *)status
           previousLabelOrAppVersion:(NSString *)prevLabelOrAppVersion
               previousDeploymentKey:(NSString *)prevDeploymentKey
{
    NSString *requestUrl = [NSString stringWithFormat:@"%@%@", [self serverURL], @"reportStatus/deploy"];

    NSMutableDictionary *body = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
                          [self appVersion], AppVersionKey,
                          [self deploymentKey ], DeploymentKeyConfigKey,
                          nil];

    if ([self clientUniqueId]){
        [body setValue:[self clientUniqueId] forKey:ClientUniqueIDConfigKey];
    }

    if (package){
        [body setValue:[package objectForKey:LabelKey] forKey:LabelKey];
        [body setValue:[package objectForKey:AppVersionKey] forKey:AppVersionKey];

        if ([status isEqualToString:DeploymentSucceeded] || [status isEqualToString:DeploymentFailed]){
            [body setValue:status forKey:StatusKey];
        }


    }
    if (prevLabelOrAppVersion){
        [body setValue:prevLabelOrAppVersion forKey:PreviousLabelOrAppVersionKey];
    }
    if (prevDeploymentKey){
        [body setValue:prevDeploymentKey forKey:prevDeploymentKey];
    }
    NSError *error;
    NSData *postData = [NSJSONSerialization dataWithJSONObject:body options:0 error:&error];
    [[self class] peformHTTPPostRequest:requestUrl withBody:postData];
    return;
}

- (void)reportStatusDownload:(NSDictionary *)downloadedPackage
{
    NSString *requestUrl = [NSString stringWithFormat:@"%@%@", [self serverURL], @"reportStatus/download"];
    NSDictionary *body = [[NSDictionary alloc] initWithObjectsAndKeys:
                          [self clientUniqueId],ClientUniqueIDConfigKey,
                          [self deploymentKey ], DeploymentKeyConfigKey,
                          [downloadedPackage objectForKey:LabelKey],LabelKey,
                          nil];
    NSError *error;
    NSData *postData = [NSJSONSerialization dataWithJSONObject:body options:0 error:&error];
    [[self class] peformHTTPPostRequest:requestUrl withBody:postData];
    return;
}

@end
