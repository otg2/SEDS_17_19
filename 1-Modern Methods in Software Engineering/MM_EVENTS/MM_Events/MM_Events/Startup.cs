using Microsoft.Owin;
using Owin;

[assembly: OwinStartupAttribute(typeof(MM_Events.Startup))]
namespace MM_Events
{
    public partial class Startup {
        public void Configuration(IAppBuilder app) {
            ConfigureAuth(app);
        }
    }
}
